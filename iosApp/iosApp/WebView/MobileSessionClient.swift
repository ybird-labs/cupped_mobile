// MobileSessionClient.swift
// Cupped - cafe.cupped.app
//
// Exchanges a native API bearer token for a Phoenix browser
// session cookie by POSTing to /auth/mobile-session via a
// hidden WKWebView.
//
// Architecture (Turbo-iOS / Basecamp 3 pattern):
//   The client creates its OWN hidden WKWebView using
//   WebViewConfiguration.makeConfiguration(). Because
//   this shares the same WKProcessPool and WKWebsiteDataStore
//   as content WebViews (FeedView, etc.), cookies set during
//   the exchange are immediately visible everywhere.
//   CuppedWebView.Coordinator has NO exchange state — the
//   two are fully decoupled.
//
// Safety guarantees:
//   - All 5 terminal WKNavigationDelegate paths are handled
//     (didFinish, didFail, didFailProvisionalNavigation,
//     processDidTerminate, decidePolicyFor response).
//   - ContinuationBox prevents double-resume crashes.
//   - 30-second timeout prevents indefinite hangs.
//   - resolve() is idempotent — first caller wins.
//   - Hidden WKWebView and delegate are cleaned up after
//     resolution to prevent retain cycles.

import Foundation
import WebKit

/// The result of a mobile-session token exchange.
enum MobileSessionResult {
    /// The exchange succeeded — session cookies are now
    /// in the shared WKWebsiteDataStore.
    case success

    /// The exchange failed.
    /// - Parameter reason: A human-readable description
    ///   of the failure (e.g., "HTTP 401", "Exchange
    ///   timed out after 30s").
    case failure(reason: String)
}

/// Exchanges a bearer API token for a browser session
/// cookie via a hidden WKWebView.
///
/// ## How It Works
/// 1. Creates a hidden (zero-frame) WKWebView with the
///    shared ``WebViewConfiguration``.
/// 2. POSTs the bearer token to `/auth/mobile-session`
///    using ``FormURLEncoder`` for correct form encoding.
/// 3. Phoenix validates the token, sets the `_brewer_key`
///    signed session cookie, and redirects to `/feed`.
/// 4. The navigation delegate detects the redirect target
///    and resolves the `async` call with
///    ``MobileSessionResult/success`` or
///    ``MobileSessionResult/failure(reason:)``.
///
/// ## Cookie Propagation
/// Cookies land in the shared `WKWebsiteDataStore` via
/// the shared `WKProcessPool`. Content WebViews
/// (``CuppedWebView``) see them immediately — no manual
/// cookie transfer needed.
///
/// ## Usage
/// ```swift
/// let client = MobileSessionClient()
/// let result = await client.exchangeToken(
///     bearerToken,
///     baseURL: KoinHelper.shared.getBaseUrl()
/// )
/// ```
///
/// ## Lifecycle
/// Create a new instance per exchange. The hidden WKWebView
/// is cleaned up when the exchange resolves (success, failure,
/// or timeout).
@MainActor
final class MobileSessionClient: NSObject,
    WKNavigationDelegate {

    /// The hidden WKWebView used for the exchange. Nil
    /// after ``resolve(_:)`` cleans up.
    private var webView: WKWebView?

    /// Thread-safe continuation wrapper. Nil after
    /// resolution.
    private var continuationBox: ContinuationBox?

    /// The expected redirect path to match against
    /// (e.g., "/feed").
    private var targetPath: String?

    /// Safety timeout task. Cancelled on resolution.
    private var timeoutTask: Task<Void, Never>?

    // MARK: - ContinuationBox

    /// Thread-safe wrapper around `CheckedContinuation`
    /// that guarantees exactly-once resumption.
    ///
    /// Multiple WKNavigationDelegate methods may fire for
    /// the same navigation (e.g., `decidePolicyFor` cancels,
    /// then `didFailProvisionalNavigation` fires). The
    /// `NSLock` ensures only the first call to
    /// ``resumeOnce(returning:)`` actually resumes the
    /// continuation; subsequent calls are no-ops.
    private final class ContinuationBox:
        @unchecked Sendable {

        private var continuation:
            CheckedContinuation<
                MobileSessionResult, Never>?
        private let lock = NSLock()

        init(
            _ continuation:
                CheckedContinuation<
                    MobileSessionResult, Never>
        ) {
            self.continuation = continuation
        }

        /// Resumes the continuation exactly once. Thread-
        /// safe — subsequent calls are silently ignored.
        func resumeOnce(
            returning value: MobileSessionResult
        ) {
            lock.lock()
            defer { lock.unlock() }
            continuation?.resume(returning: value)
            continuation = nil
        }
    }

    // MARK: - Exchange

    /// Exchanges a bearer token for a session cookie.
    ///
    /// - Parameters:
    ///   - bearerToken: The API bearer token to exchange.
    ///   - baseURL: The Phoenix server base URL (e.g.,
    ///     `"http://localhost:4000"`).
    ///   - redirectPath: The path Phoenix should redirect
    ///     to on success. Defaults to `"/feed"`.
    /// - Returns: ``MobileSessionResult/success`` if
    ///   Phoenix redirected to `redirectPath`, or
    ///   ``MobileSessionResult/failure(reason:)`` with
    ///   a human-readable error description.
    func exchangeToken(
        _ bearerToken: String,
        baseURL: String,
        redirectPath: String = "/feed"
    ) async -> MobileSessionResult {
        let urlString =
            "\(baseURL)/auth/mobile-session"
        guard let url = URL(string: urlString)
        else {
            return .failure(
                reason: "Invalid URL: \(urlString)"
            )
        }

        // Create a zero-frame WKWebView that shares the
        // centralized config. Cookies set here propagate
        // to all content WebViews via the shared
        // WKProcessPool + WKWebsiteDataStore.
        let config =
            WebViewConfiguration.makeConfiguration()
        let wv = WKWebView(
            frame: .zero,
            configuration: config
        )
        wv.navigationDelegate = self
        self.webView = wv

        // Build the POST request with correct form
        // encoding (FormURLEncoder, not .urlQueryAllowed).
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue(
            "application/x-www-form-urlencoded",
            forHTTPHeaderField: "Content-Type"
        )
        request.httpBody = FormURLEncoder.encodeToData([
            ("token", bearerToken),
            ("redirect_to", redirectPath)
        ])

        return await withCheckedContinuation {
            continuation in
            let box = ContinuationBox(continuation)
            self.continuationBox = box
            self.targetPath = redirectPath

            // Start loading only after all terminal-state bookkeeping is in place.
            // Some failures (for example invalid local URLs) can synchronously
            // trigger delegate callbacks during `load(_:)`.
            wv.load(request)

            // If the server hangs or the network stalls,
            // this prevents the caller from awaiting
            // forever. 30s is generous for a local POST.
            self.timeoutTask = Task {
                [weak self] in
                try? await Task.sleep(
                    for: .seconds(30)
                )
                guard !Task.isCancelled else {
                    return
                }
                self?.resolve(
                    .failure(
                        reason: "Exchange timed out"
                            + " after 30s"
                    )
                )
            }
        }
    }

    // MARK: - WKNavigationDelegate
    // All 5 terminal paths are handled to guarantee the
    // continuation is always resumed.

    /// **Terminal path 1** — Navigation completed.
    ///
    /// On success, Phoenix redirects to `redirectPath`
    /// after setting the session cookie. We compare the
    /// final URL's path to detect success vs. unexpected
    /// redirect (e.g., to an error page).
    func webView(
        _ webView: WKWebView,
        didFinish navigation: WKNavigation!
    ) {
        let finalURL = webView.url
        let target = targetPath ?? "/feed"
        if let url = finalURL,
           url.path == target
           || url.path == target + "/" {
            resolve(.success)
        } else {
            let actual =
                finalURL?.absoluteString ?? "unknown"
            resolve(.failure(
                reason: "Redirected to \(actual)"
                    + " instead of \(target)"
            ))
        }
    }

    /// **Terminal path 2** — Navigation failed after the
    /// server responded (e.g., content decoding error,
    /// mid-transfer network drop).
    func webView(
        _ webView: WKWebView,
        didFail navigation: WKNavigation!,
        withError error: Error
    ) {
        resolve(.failure(
            reason: error.localizedDescription
        ))
    }

    /// **Terminal path 3** — Navigation failed before the
    /// server responded (DNS failure, TLS error, server
    /// unreachable).
    func webView(
        _ webView: WKWebView,
        didFailProvisionalNavigation
            navigation: WKNavigation!,
        withError error: Error
    ) {
        resolve(.failure(
            reason: error.localizedDescription
        ))
    }

    /// **Terminal path 4** — WebKit's content process was
    /// killed by the OS during the exchange (memory
    /// pressure).
    func webViewWebContentProcessDidTerminate(
        _ webView: WKWebView
    ) {
        resolve(.failure(
            reason: "WebView process terminated"
        ))
    }

    /// **Terminal path 5** — HTTP response status check.
    ///
    /// Inspects the HTTP status code before the page loads.
    /// 4xx/5xx responses are cancelled immediately with a
    /// precise error (e.g., "HTTP 401") rather than waiting
    /// for the error page to render. Modeled on Turbo-iOS's
    /// `ColdBootVisit` pattern.
    func webView(
        _ webView: WKWebView,
        decidePolicyFor
            navigationResponse: WKNavigationResponse,
        decisionHandler:
            @escaping (WKNavigationResponsePolicy)
                -> Void
    ) {
        if let httpResponse = navigationResponse
            .response as? HTTPURLResponse,
           httpResponse.statusCode >= 400 {
            decisionHandler(.cancel)
            resolve(.failure(
                reason: "HTTP \(httpResponse.statusCode)"
            ))
        } else {
            decisionHandler(.allow)
        }
    }

    // MARK: - Resolution

    /// Resolves the exchange with the given result.
    ///
    /// Idempotent — only the first call has any effect
    /// (subsequent calls find `continuationBox == nil`
    /// and return immediately). Cleans up all state:
    /// cancels timeout, nils delegate to break retain
    /// cycle, releases the hidden WKWebView.
    private func resolve(
        _ result: MobileSessionResult
    ) {
        guard let box = continuationBox else { return }

        // Clear retained state before resuming the continuation. The awaiting
        // caller may immediately start another exchange, and we do not want stale
        // delegate or timeout state to bleed into the next attempt.
        continuationBox = nil
        targetPath = nil
        timeoutTask?.cancel()
        timeoutTask = nil
        webView?.navigationDelegate = nil
        webView = nil
        box.resumeOnce(returning: result)
    }
}
