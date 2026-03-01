// TokenStore.swift
// Cupped - cafe.cupped.app
//
// Persists the bearer token to the iOS Keychain for
// biometric re-authentication. Separate from CookieStore
// (which handles WKWebView session cookies).
//
// The bearer token is obtained from the magic link verify
// endpoint and stored here ONLY if the user opts into
// biometric auth. On subsequent app launches, BiometricService
// authenticates the user, then TokenStore retrieves the token
// for MobileSessionClient to exchange for a session cookie.

import Foundation
import Security

/// Manages bearer token persistence in the iOS Keychain.
///
/// ## Keychain Configuration
/// - Class: `kSecClassGenericPassword`
/// - Service: `cafe.cupped.app`
/// - Account: `bearer-token`
/// - Accessibility: `kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly`
///   (requires passcode, not in backups, removed if passcode disabled)
///
/// ## Relationship to CookieStore
/// CookieStore persists WKWebView session cookies (account: `webview-cookies`).
/// TokenStore persists the API bearer token (account: `bearer-token`).
/// They share the same Keychain service but different accounts.
@MainActor
final class TokenStore {

    static let shared = TokenStore()

    private let service = "cafe.cupped.app"
    private let account = "bearer-token"

    private init() {}

    // MARK: - Save

    /// Saves a bearer token to the Keychain.
    ///
    /// Overwrites any existing token. Uses delete-then-add
    /// pattern (same as CookieStore) to avoid SecItemUpdate
    /// complexity.
    ///
    /// - Parameter token: The bearer token string to persist.
    /// - Returns: `true` if the save succeeded.
    @discardableResult
    func save(token: String) -> Bool {
        // Always delete first (idempotent)
        delete()

        guard let data = token.data(using: .utf8) else {
            return false
        }

        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecValueData as String: data,
            kSecAttrAccessible as String:
                kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly
        ]

        let status = SecItemAdd(query as CFDictionary, nil)
        return status == errSecSuccess
    }

    // MARK: - Retrieve

    /// Retrieves the stored bearer token from the Keychain.
    ///
    /// - Returns: The bearer token string, or `nil` if no
    ///   token is stored or retrieval failed.
    func retrieve() -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]

        var result: AnyObject?
        let status = SecItemCopyMatching(
            query as CFDictionary, &result
        )

        guard status == errSecSuccess,
              let data = result as? Data,
              let token = String(data: data, encoding: .utf8)
        else { return nil }

        return token
    }

    // MARK: - Delete

    /// Removes the bearer token from the Keychain.
    ///
    /// Call during logout to clear stored credentials.
    @discardableResult
    func delete() -> Bool {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]
        let status = SecItemDelete(query as CFDictionary)
        return status == errSecSuccess || status == errSecItemNotFound
    }

    // MARK: - Query

    /// Whether a bearer token exists in the Keychain.
    func hasToken() -> Bool {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecReturnData as String: false,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        return SecItemCopyMatching(
            query as CFDictionary, nil
        ) == errSecSuccess
    }
}
