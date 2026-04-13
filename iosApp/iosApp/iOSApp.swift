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
//   5. Gate root view on AuthCoordinator.isAuthenticated:
//      LoginView when not authenticated, MainTabView when
//      authenticated.
//   6. Handle deep links for magic link callbacks
//      (cupped:// custom scheme and Universal Links via
//      https://<host>/users/log-in/<token>).

import SwiftUI
import Shared

@main
struct iOSApp: App {
    private static let previewBaseUrl = "http://localhost:4000"

    /// Gates content presentation until cookie restore
    /// and auth check complete. While `false`, a canvas-
    /// colored splash is shown. This guarantees cookies are
    /// in the WKHTTPCookieStore BEFORE any WKWebView loads
    /// a URL.
    @State private var isBootstrapped = false

    /// Single source of truth for auth state. Gates root
    /// view between LoginView and MainTabView. Owned by
    /// iOSApp and passed to views that trigger auth
    /// transitions.
    @State private var authCoordinator = AuthCoordinator()

    /// Tracks app lifecycle for background cookie persist.
    @Environment(\.scenePhase) private var scenePhase

    /// Server hostname extracted from the API base URL.
    /// Used by CookieStore to filter cookies by domain.
    private let serverHost: String

    init() {
        let baseUrl = Self.resolveBaseUrl()
        KoinHelper.shared.doInitKoin(baseUrl: baseUrl)

        // Extract host for cookie domain filtering.
        self.serverHost = URL(string: baseUrl)?.host()
            ?? "localhost"
    }

    var body: some Scene {
        WindowGroup {
            ZStack(alignment: .top) {
                Group {
                    if isBootstrapped {
                        if authCoordinator.isAuthenticated {
                            MainTabView()
                                .environment(
                                    authCoordinator
                                )
                        } else {
                            LoginView { bearerToken in
                                Task {
                                    _ = await authCoordinator
                                        .exchangeAndPersist(
                                            bearerToken:
                                                bearerToken
                                        )
                                }
                            }
                        }
                    } else {
                        // Canvas-colored splash matching the
                        // design system. Uses the token directly
                        // so it stays in sync with CuppedColors.
                        Color.cuppedSurfaceApp
                            .ignoresSafeArea()
                    }
                }

                AuthFlowOverlay()
            }
            .environment(authCoordinator)
            .tint(.cuppedActionPrimary)
            .animation(.cuppedSpring, value: authCoordinator.authFlowStatus)
            .task {
                // This startup task intentionally runs before any auth-gated view
                // can present a WKWebView. Keeping restore, observer registration,
                // and auth detection in one ordered block avoids session races that
                // would be hard to spot in testing.

                // Step 1: Restore cookies from Keychain
                // into WKHTTPCookieStore BEFORE any
                // WebView renders.
                let restoredSession = await CookieStore.shared.restoreCookies(
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

                // Step 3: Check for existing session.
                if restoredSession {
                    // Cookies were restored in Step 1 —
                    // user has an active session.
                    authCoordinator.isAuthenticated = true
                }

                // Step 4: Ungate content — views may now
                // safely render.
                isBootstrapped = true
            }
            .onOpenURL { url in
                // Deep links can arrive while the app is already running or during
                // cold start handoff from the system. Delegate all parsing to one
                // place so both entry points follow the same auth rules.
                handleDeepLink(url)
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

    private static func resolveBaseUrl() -> String {
        // SwiftUI Previews can launch the app without the
        // expected xcconfig-backed Info.plist substitution.
        // Fall back to localhost so component previews can
        // render without trapping in App.init().
        let rawValue = (Bundle.main.infoDictionary?["APIBaseURL"] as? String)?
            .trimmingCharacters(in: .whitespacesAndNewlines)

        if let rawValue,
           isUsableBaseUrl(rawValue) {
            return rawValue
        }

        if isRunningInPreviews {
            return previewBaseUrl
        }

        fatalError(
            "APIBaseURL missing or invalid in Info.plist"
            + " – check Config.xcconfig"
        )
    }

    private static var isRunningInPreviews: Bool {
        ProcessInfo.processInfo.environment["XCODE_RUNNING_FOR_PREVIEWS"] == "1"
    }

    private static func isUsableBaseUrl(_ value: String) -> Bool {
        guard !value.isEmpty,
              !value.contains("$("),
              let url = URL(string: value),
              let scheme = url.scheme,
              let host = url.host,
              !scheme.isEmpty,
              !host.isEmpty else {
            return false
        }

        return true
    }

    // MARK: - Deep Link Handling

    /// Parses magic link callback URLs and triggers token
    /// verification + exchange.
    ///
    /// Supports two URL formats:
    /// - Universal link:
    ///   `https://<host>/users/log-in/<token>`
    ///   where `<host>` is derived from `API_BASE_URL`
    ///   (per xcconfig) or `cupped.cafe` (future prod).
    /// - Custom scheme:
    ///   `cupped://auth/callback?token=xxx`
    private func handleDeepLink(_ url: URL) {
        // Ignore unrecognized URLs rather than surfacing an auth error. The app
        // may receive links for unrelated app features in the future.
        guard let components = URLComponents(
            url: url,
            resolvingAgainstBaseURL: false
        ) else { return }

        // Hosts that this build considers valid for
        // universal links. `serverHost` comes from
        // API_BASE_URL (per-environment xcconfig).
        // `cupped.cafe` is included so the app works
        // immediately when production migrates there.
        let validHosts: Set<String> = [
            serverHost.lowercased(), "cupped.cafe".lowercased(),
        ].filter { !$0.isEmpty }.reduce(into: []) {
            $0.insert($1)
        }

        // Universal link:
        // https://<host>/users/log-in/<token>
        // The token is the last path component (base64url-
        // encoded), NOT a query parameter.
        if components.scheme?.lowercased() == "https",
           let host = components.host?.lowercased(),
           validHosts.contains(host),
           components.path.hasPrefix("/users/log-in/")
        {
            let token = url.lastPathComponent
            guard !token.isEmpty,
                  token != "log-in"
            else { return }

            Task {
                await authCoordinator.handleMagicLinkToken(token)
            }
            return
        }

        // Custom scheme fallback:
        // cupped://auth/callback?token=xxx
        if components.scheme == "cupped",
           components.host == "auth",
           components.path == "/callback",
           let token = components.queryItems?
               .first(where: { $0.name == "token" })?
               .value,
           !token.isEmpty
        {
            Task {
                await authCoordinator.handleMagicLinkToken(token)
            }
            return
        }
    }
}
