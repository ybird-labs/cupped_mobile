// CookieStore.swift
// Cupped - cafe.cupped.app
//
// Persists WKWebView session cookies to the iOS Keychain
// so they survive app termination and device restarts.
// Uses HTTPCookie.properties (Apple's round-trip mechanism)
// for serialization — not a manual Codable DTO — to avoid
// silent data loss when Apple adds new cookie property keys.
//
// Persistence triggers:
//   1. WKHTTPCookieStoreObserver (incremental, on change)
//   2. scenePhase .background (belt-and-suspenders)
//
// Restore happens once at app launch, BEFORE any WKWebView
// navigates (gated by iOSApp.isBootstrapped).

import Foundation
import WebKit
import Security

/// Manages WKWebView cookie persistence to the iOS
/// Keychain.
///
/// ## Serialization Strategy
/// Cookies are serialized via `HTTPCookie.properties` —
/// Apple's documented round-trip mechanism — and archived
/// with `NSKeyedArchiver`. This captures **all** cookie
/// properties, including any future ones Apple adds,
/// avoiding the silent data loss risk of a manual Codable
/// DTO.
///
/// ## Stale Cookie Defense
/// ``persistCookies(from:)`` **always** deletes existing
/// Keychain data before conditionally writing new data.
/// This ensures that an empty cookie set (e.g., after
/// logout) clears the Keychain rather than leaving stale
/// data that would be restored on next launch.
///
/// ## Keychain Configuration
/// - Class: `kSecClassGenericPassword`
/// - Service: `cafe.cupped.app`
/// - Account: `webview-cookies`
/// - Accessibility:
///   `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`
///   (available after first device unlock; not included
///   in backups since session cookies would be stale
///   after restore)
///
/// ## Thread Safety
/// Keychain operations are thread-safe (backed by SQLite).
/// `WKHTTPCookieStore` operations must be awaited.
final class CookieStore: NSObject,
    WKHTTPCookieStoreObserver {

    /// Shared singleton. Required because
    /// `WKHTTPCookieStoreObserver` is a class protocol
    /// and the observer must outlive individual views.
    static let shared = CookieStore()

    /// Keychain service identifier (matches bundle ID).
    private let service = "cafe.cupped.app"

    /// Keychain account key for the cookie data item.
    private let account = "webview-cookies"

    /// Server host used to filter cookies during
    /// persistence. Only cookies whose domain matches
    /// this host (or vice versa, for leading-dot domains)
    /// are saved. Set via ``startObserving(cookieStore:targetHost:)``.
    private var targetHost: String?

    /// Debounce task for coalescing rapid cookie mutations.
    /// Cancelled and replaced on each `cookiesDidChange`
    /// call so that only the final mutation in a burst
    /// triggers a Keychain write.
    private var debounceTask: Task<Void, Never>?

    private override init() {
        super.init()
    }

    // MARK: - Setup

    /// Registers this instance as a
    /// `WKHTTPCookieStoreObserver` for incremental saves.
    ///
    /// Call once during app bootstrap, **after** cookies
    /// have been restored via ``restoreCookies(to:)``.
    ///
    /// - Parameters:
    ///   - cookieStore: The centralized
    ///     `WKHTTPCookieStore` from
    ///     ``WebViewConfiguration/cookieStore``.
    ///   - targetHost: The server hostname to filter
    ///     cookies against (e.g., `"localhost"` or
    ///     `"cupped.cafe"`).
    func startObserving(
        cookieStore: WKHTTPCookieStore,
        targetHost: String
    ) {
        self.targetHost = targetHost
        cookieStore.add(self)
    }

    // MARK: - WKHTTPCookieStoreObserver

    /// Called by WebKit whenever cookies change in the
    /// observed store. Debounces rapid mutations (e.g.,
    /// multiple Set-Cookie headers in a single response)
    /// into a single Keychain write after 500 ms of
    /// inactivity. This avoids transient memory spikes
    /// from repeated `NSKeyedArchiver` serialization
    /// while still closing the race condition where the
    /// app is killed before a background save.
    func cookiesDidChange(
        in cookieStore: WKHTTPCookieStore
    ) {
        debounceTask?.cancel()
        debounceTask = Task {
            try? await Task.sleep(for: .milliseconds(500))
            guard !Task.isCancelled else { return }
            await persistCookies(from: cookieStore)
        }
    }

    // MARK: - Query

    /// Synchronous check for whether persisted cookie data
    /// exists in the Keychain.
    ///
    /// Used by the DevAuthView gate to skip the dev auth
    /// screen when a previous session was persisted
    /// successfully.
    ///
    /// - Returns: `true` if a Keychain item exists for the
    ///   cookie data (regardless of whether the cookies
    ///   are still valid).
    func hasPersistedCookies() -> Bool {
        let query: [String: Any] = [
            kSecClass as String:
                kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecReturnData as String: false,
            kSecMatchLimit as String:
                kSecMatchLimitOne
        ]
        return SecItemCopyMatching(
            query as CFDictionary, nil
        ) == errSecSuccess
    }

    // MARK: - Persist

    /// Saves current WKWebView cookies to the Keychain.
    ///
    /// Filters cookies by domain and expiration, then
    /// serializes via `HTTPCookie.properties` +
    /// `NSKeyedArchiver`. Existing Keychain data is
    /// **always** deleted first to prevent stale cookie
    /// resurrection.
    ///
    /// - Parameter cookieStore: The `WKHTTPCookieStore`
    ///   to read cookies from (use
    ///   ``WebViewConfiguration/cookieStore``).
    func persistCookies(
        from cookieStore: WKHTTPCookieStore
    ) async {
        let cookies = await cookieStore.allCookies()

        // Only persist cookies for our server domain.
        // This avoids saving third-party tracking cookies
        // or cookies from OAuth providers embedded in
        // the WebView.
        let domainCookies: [HTTPCookie]
        if let host = targetHost {
            domainCookies = cookies.filter {
                $0.domain.contains(host)
                || host.contains($0.domain)
            }
        } else {
            domainCookies = cookies
        }

        // Discard cookies that have already expired.
        // Session cookies (no expiresDate) are kept.
        let valid = domainCookies.filter { cookie in
            if let expires = cookie.expiresDate {
                return expires > Date()
            }
            return true
        }

        // CRITICAL: Always delete before the emptiness
        // guard. If all cookies are gone (logout, expiry),
        // this clears the stale Keychain data. Without
        // this, the guard below would return early and
        // the old cookies would be restored on next launch.
        let deleteQuery: [String: Any] = [
            kSecClass as String:
                kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]
        SecItemDelete(deleteQuery as CFDictionary)

        guard !valid.isEmpty else { return }

        // Serialize using HTTPCookie.properties — Apple's
        // documented round-trip mechanism. This captures
        // ALL cookie attributes including any future ones,
        // unlike a manual Codable DTO which would silently
        // drop unknown properties.
        let props = valid.compactMap { $0.properties }
        guard let data = try? NSKeyedArchiver
            .archivedData(
                withRootObject: props,
                requiringSecureCoding: false
            ) else { return }

        let addQuery: [String: Any] = [
            kSecClass as String:
                kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecValueData as String: data,
            kSecAttrAccessible as String:
                kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        ]
        SecItemAdd(addQuery as CFDictionary, nil)
    }

    // MARK: - Restore

    /// Restores persisted cookies from the Keychain into
    /// the WKHTTPCookieStore.
    ///
    /// Must be called **before** any WKWebView loads a URL.
    /// Cookies are filtered for expiration on restore to
    /// avoid injecting stale sessions.
    ///
    /// - Parameter cookieStore: The `WKHTTPCookieStore` to
    ///   inject cookies into (use
    ///   ``WebViewConfiguration/cookieStore``).
    /// - Returns: `true` when at least one still-valid cookie
    ///   was restored into WebKit.
    func restoreCookies(
        to cookieStore: WKHTTPCookieStore
    ) async -> Bool {
        let query: [String: Any] = [
            kSecClass as String:
                kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecReturnData as String: true,
            kSecMatchLimit as String:
                kSecMatchLimitOne
        ]

        var result: AnyObject?
        let status = SecItemCopyMatching(
            query as CFDictionary, &result
        )

        guard status == errSecSuccess,
              let data = result as? Data,
              let props = try? NSKeyedUnarchiver
                   .unarchivedObject(
                      ofClasses: [
                          NSArray.self,
                          NSDictionary.self,
                          NSString.self,
                          NSNumber.self,
                          NSDate.self
                      ],
                       from: data
                   ) as? [[HTTPCookiePropertyKey: Any]]
        else { return false }

        // Discard cookies that expired while the app was
        // not running. Session cookies (no expiresDate)
        // are kept — the server will reject them if the
        // session was invalidated server-side.
        let now = Date()
        let cookies = props.compactMap {
            HTTPCookie(properties: $0)
        }.filter { cookie in
            if let expires = cookie.expiresDate {
                return expires > now
            }
            return true
        }

        // If the persisted blob exists but contains no
        // restorable cookies, clear it so future launches do
        // not mistake stale data for an active session.
        guard !cookies.isEmpty else {
            clearPersistedCookies()
            return false
        }

        for cookie in cookies {
            await cookieStore.setCookie(cookie)
        }

        return true
    }

    // MARK: - Clear

    /// Removes all persisted cookie data from the Keychain.
    ///
    /// Call during logout to ensure stale sessions are not
    /// restored on next launch.
    func clearPersistedCookies() {
        // Cancel any pending debounced write so it cannot
        // run after the clear and re-persist stale cookies.
        debounceTask?.cancel()
        debounceTask = nil

        let query: [String: Any] = [
            kSecClass as String:
                kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]
        SecItemDelete(query as CFDictionary)
    }
}
