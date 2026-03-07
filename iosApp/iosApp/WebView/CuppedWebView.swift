// CuppedWebView.swift
// Cupped - cafe.cupped.app
//
// UIViewRepresentable wrapper around WKWebView for use in
// SwiftUI. Loads a URL once in makeUIView and delegates all
// subsequent navigation to WKWebView's internal engine.
// updateUIView is intentionally empty — see docs below.
//
// Architecture: This view handles ONLY content display.
// Authentication (mobile-session exchange) is handled by
// MobileSessionClient, which owns a separate hidden
// WKWebView. The two share cookies via the centralized
// WKProcessPool in WebViewConfiguration.

import SwiftUI
import WebKit
import Shared

private extension UIColor {
    convenience init(_ token: ColorToken) {
        self.init(
            red: CGFloat(token.red),
            green: CGFloat(token.green),
            blue: CGFloat(token.blue),
            alpha: CGFloat(token.alpha)
        )
    }
}

/// A SwiftUI wrapper around `WKWebView` that loads a
/// Phoenix LiveView page.
///
/// ## One-Time Load, No Re-Navigation
/// The provided `url` is loaded exactly once in
/// ``makeUIView(context:)``. ``updateUIView(_:context:)``
/// is a deliberate no-op because WKWebView manages its
/// own navigation state (redirects, form submissions,
/// link taps). Reloading on URL mismatch would cause
/// redirect loops when Phoenix redirects
/// `/feed` -> `/users/log-in`.
///
/// For programmatic navigation after initial load, use
/// ``Coordinator/navigate(to:)`` instead of changing the
/// URL binding.
///
/// ## Configuration
/// All instances use ``WebViewConfiguration/makeConfiguration()``
/// ensuring shared `WKProcessPool` and `WKWebsiteDataStore`.
///
/// ## Features
/// - Canvas background color #F8FAFC (WEBV-10)
/// - Content process crash recovery via automatic reload
///   (WEBV-08)
/// - Pull-to-refresh via `UIRefreshControl` (WEBV-09,
///   added in Task 4)
/// - KVO-based state bridging to ``WebViewState``
struct CuppedWebView: UIViewRepresentable {
    /// The initial URL to load. Loaded once; subsequent
    /// navigation is handled by WKWebView internally.
    let url: URL

    /// Observable state updated by the Coordinator via KVO.
    @ObservedObject var state: WebViewState

    func makeUIView(context: Context) -> WKWebView {
        let webView = WKWebView(
            frame: .zero,
            configuration:
                WebViewConfiguration.makeConfiguration()
        )

        // Set the under-page bounce area to the app's
        // canvas color so overscroll matches the design
        // system rather than showing default white.
        let canvasColor = UIColor(CuppedColors.shared.surfaceApp)
        webView.underPageBackgroundColor = canvasColor
        webView.isOpaque = false
        webView.backgroundColor = canvasColor

        // Wire the coordinator as navigation delegate and
        // give it a weak reference for programmatic
        // navigation and pull-to-refresh.
        webView.navigationDelegate = context.coordinator
        context.coordinator.webView = webView
        context.coordinator.observe(webView)

        // WEBV-09: Pull-to-refresh via UIRefreshControl.
        // Uses scrollView.refreshControl (proper UIKit API)
        // rather than addSubview. The coordinator's weak
        // webView reference handles the reload — no
        // superview chain walking needed.
        let refreshControl = UIRefreshControl()
        refreshControl.addTarget(
            context.coordinator,
            action: #selector(Coordinator.handleRefresh(_:)),
            for: .valueChanged
        )
        webView.scrollView.refreshControl = refreshControl

        // One-time initial load. This is the ONLY place
        // a URL load is triggered. updateUIView does NOT
        // reload — see type-level documentation.
        webView.load(URLRequest(url: url))

        return webView
    }

    /// Intentionally empty.
    ///
    /// WKWebView manages its own navigation lifecycle.
    /// Re-loading here based on URL comparison would fight
    /// server-side redirects (e.g., `/feed` -> `/users/log-in`)
    /// and cause infinite redirect loops.
    func updateUIView(
        _ webView: WKWebView,
        context: Context
    ) {}

    /// Breaks the strong reference chain
    /// WKWebView -> navigationDelegate -> Coordinator -> WebViewState
    /// when the view is removed from the hierarchy.
    /// Without this, the WKWebView retains its
    /// `navigationDelegate` strongly, preventing deallocation.
    static func dismantleUIView(
        _ webView: WKWebView,
        coordinator: Coordinator
    ) {
        webView.stopLoading()
        webView.navigationDelegate = nil
        coordinator.observations.removeAll()
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(state: state)
    }

    // MARK: - Coordinator

    /// Navigation delegate and KVO bridge for the content
    /// WKWebView.
    ///
    /// Responsibilities:
    /// - Forwards WKWebView KVO properties (isLoading,
    ///   estimatedProgress, canGoBack, canGoForward, url)
    ///   to ``WebViewState`` for SwiftUI consumption.
    /// - Handles content process termination with automatic
    ///   reload (WEBV-08).
    /// - Provides ``navigate(to:)`` for programmatic
    ///   navigation.
    /// - Manages pull-to-refresh (added in Task 4).
    ///
    /// > Important: This Coordinator handles **only**
    /// > content navigation. Mobile-session exchange lives
    /// > in ``MobileSessionClient``, which owns its own
    /// > hidden WKWebView. No continuation or exchange
    /// > state exists here.
    final class Coordinator: NSObject, WKNavigationDelegate {
        /// The observable state bridged to SwiftUI.
        let state: WebViewState

        /// Weak reference to the managed WKWebView. Used
        /// for programmatic navigation and pull-to-refresh.
        weak var webView: WKWebView?

        fileprivate var observations: [NSKeyValueObservation] = []

        init(state: WebViewState) {
            self.state = state
        }

        /// Installs KVO observers on the web view and
        /// forwards property changes to ``state``.
        ///
        /// Observations are stored to keep them alive for
        /// the lifetime of the Coordinator. Each observer
        /// dispatches to `@MainActor` because KVO callbacks
        /// may fire on arbitrary queues.
        ///
        /// - Parameter webView: The WKWebView to observe.
        func observe(_ webView: WKWebView) {
            observations = [
                webView.observe(\.isLoading) {
                    [weak self] wv, _ in
                    Task { @MainActor in
                        self?.state.isLoading = wv.isLoading
                        // Dismiss pull-to-refresh spinner when
                        // page load completes (WEBV-09).
                        if !wv.isLoading {
                            wv.scrollView.refreshControl?
                                .endRefreshing()
                        }
                    }
                },
                webView.observe(\.estimatedProgress) {
                    [weak self] wv, _ in
                    Task { @MainActor in
                        self?.state.estimatedProgress =
                            wv.estimatedProgress
                    }
                },
                webView.observe(\.canGoBack) {
                    [weak self] wv, _ in
                    Task { @MainActor in
                        self?.state.canGoBack = wv.canGoBack
                    }
                },
                webView.observe(\.canGoForward) {
                    [weak self] wv, _ in
                    Task { @MainActor in
                        self?.state.canGoForward =
                            wv.canGoForward
                    }
                },
                webView.observe(\.url) {
                    [weak self] wv, _ in
                    Task { @MainActor in
                        self?.state.currentURL = wv.url
                    }
                }
            ]
        }

        /// Handles pull-to-refresh by reloading the page.
        ///
        /// Triggered by `UIRefreshControl`. The spinner is
        /// dismissed automatically by the `isLoading` KVO
        /// observer when the reload completes.
        ///
        /// - Parameter sender: The refresh control that
        ///   triggered the action.
        @objc func handleRefresh(_ sender: UIRefreshControl) {
            webView?.reload()
        }

        /// Loads a new URL in the web view programmatically.
        ///
        /// Use this for navigation triggered by app logic
        /// (e.g., after authentication). Do **not** change
        /// the `url` property on ``CuppedWebView`` — that
        /// would have no effect because `updateUIView` is
        /// a no-op.
        ///
        /// - Parameter url: The URL to navigate to.
        @MainActor
        func navigate(to url: URL) {
            webView?.load(URLRequest(url: url))
        }

        /// Automatically reloads the page when WebKit's
        /// content process is terminated by the OS (e.g.,
        /// due to memory pressure). Without this, the web
        /// view would show a blank white screen (WEBV-08).
        func webViewWebContentProcessDidTerminate(
            _ webView: WKWebView
        ) {
            let usage = Self.formattedMemoryUsage()
            print(
                "[CuppedWebView] WebContent process terminated. "
                + "App memory at time of crash: \(usage). "
                + "Reloading."
            )
            webView.reload()
        }

        /// Returns the current app memory footprint as a
        /// human-readable string (e.g. "142.3 MB").
        /// Uses `task_info` to read `phys_footprint`, the
        /// same metric the OS jetsam monitor uses.
        private static func formattedMemoryUsage() -> String {
            var info = task_vm_info_data_t()
            var count = mach_msg_type_number_t(
                MemoryLayout<task_vm_info_data_t>.size
            ) / 4
            let result = withUnsafeMutablePointer(to: &info) {
                $0.withMemoryRebound(
                    to: integer_t.self,
                    capacity: Int(count)
                ) {
                    task_info(
                        mach_task_self_,
                        task_flavor_t(TASK_VM_INFO),
                        $0,
                        &count
                    )
                }
            }
            guard result == KERN_SUCCESS else {
                return "unknown"
            }
            let bytes = Double(info.phys_footprint)
            let mb = bytes / (1024 * 1024)
            return String(format: "%.1f MB", mb)
        }

        /// Records navigation errors (post-commit) in the
        /// observable state for UI display.
        func webView(
            _ webView: WKWebView,
            didFail navigation: WKNavigation!,
            withError error: Error
        ) {
            Task { @MainActor [weak self] in
                self?.state.error = error
            }
        }

        /// Records provisional navigation errors
        /// (pre-commit, e.g., DNS failure) in the
        /// observable state for UI display.
        func webView(
            _ webView: WKWebView,
            didFailProvisionalNavigation
                navigation: WKNavigation!,
            withError error: Error
        ) {
            Task { @MainActor [weak self] in
                self?.state.error = error
            }
        }
    }
}
