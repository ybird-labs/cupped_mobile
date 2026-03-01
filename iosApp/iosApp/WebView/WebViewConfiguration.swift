// WebViewConfiguration.swift
// Cupped - cafe.cupped.app
//
// Single source of truth for all WKWebView configuration.
// Every WebView in the app — content views, hidden exchange
// views — must obtain its WKWebViewConfiguration from here.
// This guarantees cookie sharing (via shared WKProcessPool)
// and a single WKWebsiteDataStore reference for all cookie
// read/write/persist operations.

import WebKit

/// Centralized WebView configuration provider.
///
/// All WKWebView instances in the app **must** use
/// ``makeConfiguration()`` to obtain their configuration.
/// This ensures:
/// - A single ``WKProcessPool`` across all web views,
///   enabling cookie sharing between tabs and the hidden
///   session-exchange web view (WEBV-02).
/// - A single ``WKWebsiteDataStore`` reference, preventing
///   divergent cookie state.
/// - A consistent custom User-Agent identifying the native
///   app to the Phoenix server (WEBV-05).
///
/// > Important: No other code in the app should call
/// > `WKWebsiteDataStore.default()` directly. Use
/// > ``dataStore`` or ``cookieStore`` instead.
enum WebViewConfiguration {

    // MARK: - Shared Singletons

    /// Single process pool shared across all WebView
    /// instances.
    ///
    /// WKProcessPool manages the WebKit network process.
    /// Sharing one instance across all web views ensures
    /// cookies set in one (e.g., the hidden exchange view)
    /// are immediately visible in others (e.g., FeedView).
    static let processPool = WKProcessPool()

    /// Centralized website data store.
    ///
    /// All cookie read, write, and observation operations
    /// flow through this single reference. Using
    /// `WKWebsiteDataStore.default()` elsewhere would
    /// create a second reference that could diverge.
    static let dataStore = WKWebsiteDataStore.default()

    /// Convenience accessor for the HTTP cookie store.
    ///
    /// Always derived from ``dataStore`` — never a
    /// standalone reference. Use this for
    /// `WKHTTPCookieStore` operations (persist, restore,
    /// observe).
    static var cookieStore: WKHTTPCookieStore {
        dataStore.httpCookieStore
    }

    // MARK: - Factory

    /// Creates a new `WKWebViewConfiguration` wired to the
    /// shared process pool, data store, custom User-Agent,
    /// and inline media settings.
    ///
    /// Each call returns a **new** configuration instance
    /// (WKWebView requires its own), but all instances share
    /// the same ``processPool`` and ``dataStore`` singletons.
    ///
    /// - Returns: A fully configured
    ///   `WKWebViewConfiguration` ready for use with
    ///   `WKWebView(frame:configuration:)`.
    static func makeConfiguration() -> WKWebViewConfiguration {
        let config = WKWebViewConfiguration()
        config.processPool = processPool
        config.websiteDataStore = dataStore

        // Append custom identifier to WebKit's default
        // User-Agent so Phoenix can distinguish native app
        // requests from regular browser traffic. The full
        // UA becomes:
        // "Mozilla/5.0 ... CuppedMobile/iOS/1.0 (build 1)"
        let version = Bundle.main.infoDictionary?[
            "CFBundleShortVersionString"
        ] as? String ?? "0.0"
        let build = Bundle.main.infoDictionary?[
            "CFBundleVersion"
        ] as? String ?? "0"
        config.applicationNameForUserAgent =
            "CuppedMobile/iOS/\(version) (build \(build))"

        // LiveView pages may embed video content; allow
        // inline playback rather than forcing fullscreen.
        config.allowsInlineMediaPlayback = true

        return config
    }
}
