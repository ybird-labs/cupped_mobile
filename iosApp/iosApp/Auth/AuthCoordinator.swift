// AuthCoordinator.swift
// Cupped - cafe.cupped.app
//
// Orchestrates the full authentication flow: deep link handling,
// bearer → session cookie exchange, biometric re-auth, and logout.
//
// Bridges KMP AuthViewModel (token verification) with iOS-side
// MobileSessionClient (cookie exchange), CookieStore (Keychain
// persistence), and TokenStore (bearer token persistence).
//
// ## Architecture
// AuthCoordinator is the single source of truth for "is the user
// authenticated?" in the app. iOSApp.swift reads `isAuthenticated`
// to gate between LoginView and MainTabView. The coordinator does
// NOT own any UI — it's a pure state machine.
//
// ## Flows
// 1. **Email login:** LoginView → onAuthenticated(bearerToken) →
//    exchangeAndPersist → isAuthenticated = true
// 2. **Deep link:** .onOpenURL → handleMagicLinkToken → verifyToken
//    → exchangeAndPersist → isAuthenticated = true
// 3. **Biometric re-auth:** iOSApp bootstrap → BiometricService →
//    TokenStore.retrieve → exchangeAndPersist → isAuthenticated = true
// 4. **Logout:** Clear cookies, tokens, biometric flag →
//    isAuthenticated = false

import Foundation
import Shared
import WebKit
import Observation

/// Orchestrates authentication state for the entire app.
///
/// Created once by `iOSApp` and passed into views that need
/// to trigger auth transitions (login, logout). Uses the Swift
/// Observation framework (`@Observable`) for automatic SwiftUI
/// view invalidation.
@MainActor @Observable
final class AuthCoordinator {

    // MARK: - Published State

    /// Whether the user has an active session. Gates the
    /// root view between LoginView and MainTabView.
    var isAuthenticated = false

    /// Whether a token exchange is currently in progress.
    /// Used to prevent double-taps and show loading UI.
    var isExchanging = false

    /// The last exchange error message, if any. Cleared on
    /// the next successful exchange or login attempt.
    var exchangeError: String?

    /// Monotonically increasing counter incremented on each
    /// logout. Used by `exchangeAndPersist` to detect when
    /// a logout occurred while an exchange was in-flight,
    /// preventing stale completions from re-authenticating.
    private var logoutGeneration: Int = 0

    // MARK: - Exchange

    /// Exchanges a bearer token for a session cookie and
    /// persists both to Keychain.
    ///
    /// ## Steps
    /// 1. Create a fresh `MobileSessionClient`
    /// 2. Exchange bearer token → Phoenix session cookie
    /// 3. Persist cookies to Keychain via `CookieStore`
    /// 4. Save bearer token via `TokenStore` (for biometric
    ///    re-auth on next launch)
    /// 5. Set `isAuthenticated = true`
    ///
    /// - Parameter bearerToken: The API bearer token from
    ///   KMP AuthViewModel's Authenticated state.
    func exchangeAndPersist(bearerToken: String) async {
        guard !isExchanging else { return }
        isExchanging = true
        exchangeError = nil

        // Capture the current logout generation before any
        // async work. If logout() fires while we're awaiting
        // the exchange, the generation will have incremented
        // and we must discard the result.
        let generation = logoutGeneration

        defer { isExchanging = false }

        let client = MobileSessionClient()
        let baseURL = KoinHelper.shared.getBaseUrl()
        let result = await client.exchangeToken(
            bearerToken,
            baseURL: baseURL
        )

        // Bail out if a logout occurred during the exchange.
        guard logoutGeneration == generation else { return }

        switch result {
        case .success:
            // Persist cookies to Keychain so they survive
            // app termination.
            await CookieStore.shared.persistCookies(
                from: WebViewConfiguration.cookieStore
            )

            // Re-check after second await — logout may have
            // fired between the exchange and cookie persist.
            guard logoutGeneration == generation else { return }

            // Save bearer token for biometric re-auth.
            TokenStore.shared.save(token: bearerToken)

            isAuthenticated = true

        case .failure(let reason):
            exchangeError = reason
        }
    }

    // MARK: - Deep Link Handling

    /// Handles a magic link token received via deep link.
    ///
    /// Creates a fresh KMP `AuthViewModel`, calls `verifyToken`,
    /// then polls until the state transitions to `Authenticated`
    /// or `Error` (with a 30-second timeout).
    ///
    /// On success, delegates to ``exchangeAndPersist(bearerToken:)``
    /// for the cookie exchange.
    ///
    /// - Parameter token: The magic link token from the URL
    ///   query parameter.
    func handleMagicLinkToken(_ token: String) async {
        guard !isExchanging else { return }
        isExchanging = true
        exchangeError = nil

        defer { isExchanging = false }

        let authVM = KoinHelper.shared.makeAuthViewModel()
        // Ensure the KMP ViewModel's coroutine scope is
        // cancelled when we're done, regardless of how we
        // exit. Without this, the viewModelScope lingers
        // because the VM was created as a plain local (not
        // managed by @StateViewModel).
        defer { authVM.clear() }

        authVM.verifyToken(token: token)

        // Poll for state transition. The KMP ViewModel
        // processes the token asynchronously on its own
        // coroutine scope. We poll from Swift because
        // KMP StateFlow collection across the Swift/Kotlin
        // bridge is complex and polling is simpler for a
        // one-shot operation.
        let timeout: UInt64 = 30_000_000_000 // 30 seconds
        let interval: UInt64 = 100_000_000   // 100ms
        var elapsed: UInt64 = 0

        while elapsed < timeout {
            // Respect structured concurrency: if the parent
            // Task is cancelled (e.g., view disappeared),
            // stop polling instead of running until timeout.
            if Task.isCancelled { return }

            let state = authVM.uiState.value

            if let authenticated = state
                as? AuthUiStateAuthenticated {
                // Re-enter without the isExchanging guard
                // by calling the exchange directly.
                isExchanging = false
                await exchangeAndPersist(
                    bearerToken: authenticated.bearerToken
                )
                return
            }

            if let error = state as? AuthUiStateError {
                exchangeError = error.message
                return
            }

            try? await Task.sleep(nanoseconds: interval)
            elapsed += interval
        }

        exchangeError = "Magic link verification timed out"
    }

    // MARK: - Logout

    /// Clears all authentication state: cookies, tokens,
    /// biometric preference, and WKWebView cookie store.
    ///
    /// After logout, the user sees LoginView.
    func logout() async {
        // Increment generation first so any in-flight
        // exchangeAndPersist call will detect the logout
        // and bail out before persisting.
        logoutGeneration += 1

        // 1. Clear WKWebView cookies from the shared store.
        let store = WebViewConfiguration.cookieStore
        let cookies = await store.allCookies()
        for cookie in cookies {
            await store.deleteCookie(cookie)
        }

        // 2. Clear persisted cookies from Keychain.
        CookieStore.shared.clearPersistedCookies()

        // 3. Clear bearer token from Keychain.
        TokenStore.shared.delete()

        // 4. Disable biometric auth preference.
        BiometricService.shared.isEnabled = false

        // 5. Update auth state — triggers UI transition
        // back to LoginView.
        isAuthenticated = false
        exchangeError = nil
    }
}
