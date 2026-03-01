// iOSApp.swift
// Cupped - cafe.cupped.app
//
// The @main entry point for the iOS app. Responsibilities:
//   1. Initialize KMP/Koin with the API base URL from
//      Config.xcconfig (via Info.plist).
//   2. Restore persisted cookies from Keychain BEFORE
//      any WKWebView navigates (bootstrap gate).
//   3. Start the WKHTTPCookieStoreObserver for incremental
//      cookie saves.
//   4. Persist cookies to Keychain when the app moves to
//      the background (belt-and-suspenders with observer).
//   5. In DEBUG builds, gate on DevAuthView when no
//      persisted cookies exist (added in Task 7).

import SwiftUI
import Shared

@main
struct iOSApp: App {
    /// Gates content presentation until cookie restore
    /// completes. While `false`, a canvas-colored splash
    /// is shown instead of MainTabView. This guarantees
    /// cookies are in the WKHTTPCookieStore BEFORE any
    /// WKWebView loads a URL.
    @State private var isBootstrapped = false

    /// Tracks app lifecycle for background cookie persist.
    @Environment(\.scenePhase) private var scenePhase

    /// Server hostname extracted from the API base URL.
    /// Used by CookieStore to filter cookies by domain.
    private let serverHost: String

    init() {
        // Read the API base URL injected from
        // Config.xcconfig -> Info.plist -> APIBaseURL.
        // This is the same URL used by CuppedApiClient
        // (KMP) and WebView URL construction (Swift).
        guard let baseUrl = Bundle.main.infoDictionary?[
            "APIBaseURL"] as? String,
              !baseUrl.isEmpty else {
            fatalError(
                "APIBaseURL missing from Info.plist"
                + " – check Config.xcconfig"
            )
        }
        KoinHelper.shared.doInitKoin(baseUrl: baseUrl)

        // Extract host for cookie domain filtering.
        self.serverHost = URL(string: baseUrl)?.host()
            ?? "localhost"
    }

    var body: some Scene {
        WindowGroup {
            Group {
                if isBootstrapped {
                    MainTabView()
                } else {
                    // Canvas-colored splash matching the
                    // design system. Shown only during the
                    // brief async cookie restore.
                    Color(
                        red: 248 / 255,
                        green: 250 / 255,
                        blue: 252 / 255
                    )
                    .ignoresSafeArea()
                }
            }
            .task {
                // Step 1: Restore cookies from Keychain
                // into WKHTTPCookieStore BEFORE any
                // WebView renders.
                await CookieStore.shared.restoreCookies(
                    to: WebViewConfiguration.cookieStore
                )

                // Step 2: Start observing cookie changes
                // for incremental Keychain saves. This
                // closes the race condition where the app
                // is force-quit before the background save.
                CookieStore.shared.startObserving(
                    cookieStore:
                        WebViewConfiguration.cookieStore,
                    targetHost: serverHost
                )

                // Step 3: Ungate content — WebViews may
                // now safely navigate.
                isBootstrapped = true
            }
        }
        .onChange(of: scenePhase) { _, newPhase in
            if newPhase == .background {
                // Belt-and-suspenders persist alongside the
                // WKHTTPCookieStoreObserver. Ensures cookies
                // are saved even if the observer callback
                // hasn't fired for the latest changes.
                Task {
                    await CookieStore.shared.persistCookies(
                        from:
                            WebViewConfiguration.cookieStore
                    )
                }
            }
        }
    }
}
