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
import OSLog
import Shared
import WebKit
import Observation

enum AuthFlowStatus: Equatable {
    case idle
    case verifyingMagicLink
    case establishingSession
    case succeeded(message: String)
    case failed(message: String, debugDetails: String?)
}

/// Orchestrates authentication state for the entire app.
///
/// Created once by `iOSApp` and passed into views that need
/// to trigger auth transitions (login, logout). Uses the Swift
/// Observation framework (`@Observable`) for automatic SwiftUI
/// view invalidation.
@MainActor @Observable
final class AuthCoordinator {
    private static let deepLinkSuccessDwellNanoseconds: UInt64 = 1_500_000_000
    private static let logger = Logger(
        subsystem: Bundle.main.bundleIdentifier ?? "cafe.cupped.app",
        category: "Auth"
    )

    deinit {}

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

    /// User-facing status for cross-screen auth flows like
    /// deep-link sign-in. This gives LoginView enough
    /// information to show progress and retry guidance.
    var authFlowStatus: AuthFlowStatus = .idle

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
    func exchangeAndPersist(bearerToken: String) async -> Bool {
        guard !isExchanging else { return false }
        isExchanging = true
        exchangeError = nil
        let isDeepLinkFlow =
            authFlowStatus == .verifyingMagicLink
            || authFlowStatus == .establishingSession

        if !isDeepLinkFlow {
            authFlowStatus = .idle
        }

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
        guard logoutGeneration == generation else { return false }

        switch result {
        case .success:
            // Persist cookies to Keychain so they survive
            // app termination.
            await CookieStore.shared.persistCookies(
                from: WebViewConfiguration.cookieStore
            )

            // Re-check after second await — logout may have
            // fired between the exchange and cookie persist.
            guard logoutGeneration == generation else { return false }

            // Save bearer token for biometric re-auth.
            let tokenSaved = TokenStore.shared.save(token: bearerToken)
            if !tokenSaved {
                Self.logger.error("Failed to persist bearer token for biometric re-auth")
                BiometricService.shared.isEnabled = false
            }

            let shouldShowDeepLinkSuccess =
                authFlowStatus == .establishingSession

            if shouldShowDeepLinkSuccess {
                authFlowStatus = .succeeded(
                    message: "Magic link verified. You're signed in."
                )
                try? await Task.sleep(
                    nanoseconds: Self.deepLinkSuccessDwellNanoseconds
                )

                // A logout or newer auth flow may have happened during the dwell.
                guard logoutGeneration == generation else { return false }
            }

            authFlowStatus = .idle
            isAuthenticated = true
            return true

        case .failure(let reason):
            exchangeError = reason
            if authFlowStatus == .verifyingMagicLink
                || authFlowStatus == .establishingSession {
                authFlowStatus = Self.failureStatus(
                    stage: .establishingSession,
                    raw: reason
                )
            }
            return false
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
    func handleMagicLinkToken(_ token: String) async -> Bool {
        guard !isExchanging else { return false }
        isExchanging = true
        exchangeError = nil
        authFlowStatus = .verifyingMagicLink
        let generation = logoutGeneration

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
            if Task.isCancelled { return false }
            guard logoutGeneration == generation else { return false }

            let state = authVM.uiState.value

            if let authenticated = state
                as? AuthUiStateAuthenticated {
                // Re-enter without the isExchanging guard
                // by calling the exchange directly.
                authFlowStatus = .establishingSession
                isExchanging = false
                return await exchangeAndPersist(
                    bearerToken: authenticated.bearerToken
                )
            }

            if let error = state as? AuthUiStateError {
                exchangeError = error.message
                authFlowStatus = Self.failureStatus(
                    stage: .verifyingMagicLink,
                    raw: error.message
                )
                return false
            }

            try? await Task.sleep(nanoseconds: interval)
            elapsed += interval
            guard logoutGeneration == generation else { return false }
        }

        exchangeError = "Magic link verification timed out"
        authFlowStatus = Self.failureStatus(
            stage: .verifyingMagicLink,
            raw: exchangeError ?? "Magic link verification timed out"
        )
        return false
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
        authFlowStatus = .idle
    }

    func clearAuthFlowStatus() {
        authFlowStatus = .idle
    }

    private static func failureStatus(
        stage: AuthFlowStatus,
        raw: String
    ) -> AuthFlowStatus {
        .failed(
            message: userFacingMessage(for: raw, stage: stage),
            debugDetails: raw
        )
    }

    private static func userFacingMessage(
        for raw: String,
        stage: AuthFlowStatus
    ) -> String {
        let lowered = raw.lowercased()

        if lowered.contains("http 401")
            || lowered.contains("http 404")
            || lowered.contains("http 422")
            || lowered.contains("expired")
            || lowered.contains("invalid")
            || lowered.contains("timed out")
            || lowered.contains("not found") {
            return "This magic link expired or is invalid. Request a new one."
        }

        if lowered.contains("network")
            || lowered.contains("nsurlerrordomain")
            || lowered.contains("cfnetwork")
            || lowered.contains("offline")
            || lowered.contains("connection") {
            return "Unable to sign you in right now. Check your connection and try again."
        }

        if lowered.contains("redirected to") {
            return "We verified your link, but could not finish signing you in. Please try again."
        }

        if lowered.contains("http 403") || lowered.contains("http 500") {
            return "We verified your link, but could not start your session. Please try again."
        }

        if stage == .establishingSession {
            return "We verified your link, but could not finish signing you in. Please try again."
        }

        return "Something went wrong signing you in. Please try again."
    }
}
