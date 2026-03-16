// BiometricService.swift
// Cupped - cafe.cupped.app
//
// Wraps LocalAuthentication framework for Face ID / Touch ID.
// Opt-in via UserDefaults toggle — user is prompted once after
// first login and can change in settings.

import LocalAuthentication
import OSLog
import UIKit

/// Biometric authentication type available on device.
enum BiometricType {
    case faceID
    case touchID
    case none
}

/// Wraps `LAContext` for Face ID / Touch ID authentication.
///
/// ## Opt-In Model
/// Biometric auth is opt-in. The `isEnabled` flag is stored in
/// `UserDefaults` and defaults to `false`. After first successful
/// login, the app prompts the user to enable biometrics. They can
/// toggle it later in settings.
///
/// ## Flow (when enabled)
/// 1. App launches -> check `isEnabled` and `TokenStore.hasToken()`
/// 2. If both true -> present biometric prompt
/// 3. On success -> retrieve token from `TokenStore`
/// 4. Pass token to `MobileSessionClient` for cookie exchange
/// 5. Show `MainTabView`
///
/// ## Privacy
/// `NSFaceIDUsageDescription` must be set in Info.plist for Face ID.
/// Touch ID does not require a usage description.
@MainActor
final class BiometricService {

    static let shared = BiometricService()
    private static let logger = Logger(
        subsystem: Bundle.main.bundleIdentifier ?? "cafe.cupped.app",
        category: "Biometrics"
    )

    private let enabledKey = "cafe.cupped.biometric.enabled"

    private init() {}

    deinit {}

    // MARK: - Capability

    /// The type of biometric authentication available on this device.
    var availableType: BiometricType {
        let context = LAContext()
        var error: NSError?
        guard context.canEvaluatePolicy(
            .deviceOwnerAuthenticationWithBiometrics,
            error: &error
        ) else {
            return .none
        }

        switch context.biometryType {
        case .faceID: return .faceID
        case .touchID: return .touchID
        case .opticID:
            Self.logger.info(
                "Observed opticID and mapped to faceID for biometric flow. biometryType=\(String(describing: context.biometryType)) idiom=\(UIDevice.current.userInterfaceIdiom.rawValue) systemVersion=\(UIDevice.current.systemVersion)"
            )
            return .faceID // Vision Pro — treat as Face ID
        case .none: return .none
        @unknown default: return .none
        }
    }

    /// Whether biometric auth is available on this device.
    var isAvailable: Bool {
        availableType != .none
    }

    /// Human-readable name for the available biometric type.
    var biometricName: String {
        switch availableType {
        case .faceID: "Face ID"
        case .touchID: "Touch ID"
        case .none: "Biometrics"
        }
    }

    // MARK: - Opt-In Toggle

    /// Whether the user has opted into biometric authentication.
    /// Stored in UserDefaults, defaults to `false`.
    var isEnabled: Bool {
        get { UserDefaults.standard.bool(forKey: enabledKey) }
        set { UserDefaults.standard.set(newValue, forKey: enabledKey) }
    }

    /// Whether biometric auth should be attempted on app launch.
    /// Requires both: user opted in AND a token is stored.
    var shouldAttemptBiometric: Bool {
        isEnabled && isAvailable && TokenStore.shared.hasToken()
    }

    // MARK: - Authentication

    /// Presents the biometric authentication prompt.
    ///
    /// Returns the authenticated `LAContext` on success so
    /// it can be reused for Keychain operations that require
    /// biometric access control (e.g., `TokenStore.retrieve`),
    /// avoiding a double biometric prompt.
    ///
    /// - Parameter reason: The reason string shown to the user
    ///   (e.g., "Sign in to Cupped").
    /// - Returns: The authenticated `LAContext` on success,
    ///   or `nil` if the user cancelled or evaluation failed.
    func authenticate(
        reason: String = "Sign in to Cupped"
    ) async -> LAContext? {
        let context = LAContext()
        context.localizedCancelTitle = "Use Magic Link"

        do {
            let success = try await context.evaluatePolicy(
                .deviceOwnerAuthenticationWithBiometrics,
                localizedReason: reason
            )
            return success ? context : nil
        } catch {
            return nil
        }
    }
}
