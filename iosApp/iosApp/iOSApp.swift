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
//      (cupped:// and https://cupped.cafe universal links).

import SwiftUI
import Shared

@main
struct iOSApp: App {
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

    /// Whether to show the one-time biometric opt-in prompt
    /// after first successful login.
    @State private var showBiometricPrompt = false

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
                    if authCoordinator.isAuthenticated {
                        if showBiometricPrompt {
                            BiometricPromptView {
                                UserDefaults.standard.set(
                                    true,
                                    forKey:
                                        "cafe.cupped.biometric.prompted"
                                )
                                showBiometricPrompt = false
                            }
                        } else {
                            MainTabView()
                                .environment(
                                    authCoordinator
                                )
                        }
                    } else {
                        LoginView { bearerToken in
                            Task {
                                await authCoordinator
                                    .exchangeAndPersist(
                                        bearerToken:
                                            bearerToken
                                    )

                                // Show biometric prompt
                                // once after first login
                                // if biometrics are
                                // available but not yet
                                // enabled or prompted.
                                if BiometricService.shared
                                    .isAvailable,
                                   !BiometricService.shared
                                    .isEnabled,
                                   !UserDefaults.standard
                                    .bool(
                                        forKey:
                                            "cafe.cupped.biometric.prompted"
                                    )
                                {
                                    showBiometricPrompt
                                        = true
                                }
                            }
                        }
                    }
                } else {
                    // Canvas-colored splash matching the
                    // design system. Shown only during the
                    // brief async bootstrap.
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

                // Step 3: Check for existing session.
                if CookieStore.shared
                    .hasPersistedCookies() {
                    // Cookies were restored in Step 1 —
                    // user has an active session.
                    authCoordinator.isAuthenticated = true
                } else if BiometricService.shared
                    .shouldAttemptBiometric {
                    // No cookies, but user opted into
                    // biometrics and has a stored token.
                    // Attempt biometric auth → exchange.
                    let success = await BiometricService
                        .shared.authenticate()
                    if success,
                       let token = TokenStore.shared
                           .retrieve() {
                        await authCoordinator
                            .exchangeAndPersist(
                                bearerToken: token
                            )
                    }
                }

                // Step 4: Ungate content — views may now
                // safely render.
                isBootstrapped = true
            }
            .onOpenURL { url in
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

    // MARK: - Deep Link Handling

    /// Parses magic link callback URLs and triggers token
    /// verification + exchange.
    ///
    /// Supports two URL formats:
    /// - Custom scheme: `cupped://auth/callback?token=xxx`
    /// - Universal link: `https://cupped.cafe/auth/verify?token=xxx`
    private func handleDeepLink(_ url: URL) {
        guard let components = URLComponents(
            url: url,
            resolvingAgainstBaseURL: false
        ) else { return }

        // Match custom scheme: cupped://auth/callback
        let isCustomScheme =
            components.scheme == "cupped"
            && components.host == "auth"
            && components.path == "/callback"

        // Match universal link:
        // https://cupped.cafe/auth/verify
        let isUniversalLink =
            (components.scheme == "https"
             || components.scheme == "http")
            && components.host?.contains("cupped.cafe")
                == true
            && components.path == "/auth/verify"

        guard isCustomScheme || isUniversalLink else {
            return
        }

        guard let token = components.queryItems?
            .first(where: { $0.name == "token" })?
            .value,
              !token.isEmpty
        else { return }

        Task {
            await authCoordinator
                .handleMagicLinkToken(token)
        }
    }
}
