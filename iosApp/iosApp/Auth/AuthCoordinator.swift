import Foundation
import Observation
import OSLog
import Shared
import WebKit

enum AuthFlowStatus: Equatable {
    case idle
    case verifyingMagicLink
    case establishingSession
    case succeeded(message: String)
    case failed(message: String, debugDetails: String?)
}

@MainActor @Observable
final class AuthCoordinator {
    private static let deepLinkSuccessDwellNanoseconds: UInt64 = 1_500_000_000
    private static let logger = Logger(
        subsystem: Bundle.main.bundleIdentifier ?? "cafe.cupped.app",
        category: "Auth"
    )

    var isAuthenticated = false
    var isExchanging = false
    var exchangeError: String?
    var authFlowStatus: AuthFlowStatus = .idle

    private var logoutGeneration = 0

    func exchangeAndPersist(bearerToken: String) async -> Bool {
        guard !isExchanging else { return false }
        isExchanging = true
        exchangeError = nil

        let isDeepLinkFlow = authFlowStatus == .verifyingMagicLink
            || authFlowStatus == .establishingSession

        if !isDeepLinkFlow {
            authFlowStatus = .idle
        }

        let generation = logoutGeneration
        defer { isExchanging = false }

        let result = await MobileSessionClient().exchangeToken(
            bearerToken,
            baseURL: KoinHelper.shared.getBaseUrl()
        )

        guard logoutGeneration == generation else { return false }

        switch result {
        case .success:
            await CookieStore.shared.persistCookies(
                from: WebViewConfiguration.cookieStore
            )

            guard logoutGeneration == generation else { return false }

            if BiometricService.shared.isAvailable {
                let tokenSaved = TokenStore.shared.save(token: bearerToken)
                if !tokenSaved {
                    Self.logger.error(
                        "Failed to persist bearer token for biometric re-auth"
                    )
                    BiometricService.shared.isEnabled = false
                }
            }

            if authFlowStatus == .establishingSession {
                authFlowStatus = .succeeded(
                    message: "Magic link verified. You're signed in."
                )
                try? await Task.sleep(
                    nanoseconds: Self.deepLinkSuccessDwellNanoseconds
                )
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

    func handleMagicLinkToken(_ token: String) async -> Bool {
        guard !isExchanging else { return false }
        isExchanging = true
        exchangeError = nil
        authFlowStatus = .verifyingMagicLink
        let generation = logoutGeneration

        defer { isExchanging = false }

        let authViewModel = KoinHelper.shared.makeAuthViewModel()
        defer { authViewModel.clear() }

        authViewModel.verifyToken(token: token)

        let timeout: UInt64 = 30_000_000_000
        let interval: UInt64 = 100_000_000
        var elapsed: UInt64 = 0

        while elapsed < timeout {
            if Task.isCancelled { return false }
            guard logoutGeneration == generation else { return false }

            let state = authViewModel.uiState.value

            if let authenticated = state as? AuthUiStateAuthenticated {
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

    func logout() async {
        logoutGeneration += 1

        let store = WebViewConfiguration.cookieStore
        let cookies = await store.allCookies()
        for cookie in cookies {
            await store.deleteCookie(cookie)
        }

        CookieStore.shared.clearPersistedCookies()
        TokenStore.shared.delete()
        BiometricService.shared.isEnabled = false

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
