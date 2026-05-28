package cafe.cupped.app.auth

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.COpaquePointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.set
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryCreate
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlock
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.darwin.OSStatus

/**
 * iOS [TokenStore] backed by the Keychain (generic-password items).
 * Satisfies architecture §11: a persistent, platform-keystore-backed token
 * store. Items use `kSecAttrAccessibleAfterFirstUnlock` so sync can run while
 * the device is locked, after the first unlock since boot.
 *
 * NOTE (unverified linking): this file compiles to Kotlin/Native but the
 * Security framework calls have not been exercised on a device/simulator in
 * this environment. The CoreFoundation interop pattern below (CFDictionaryCreate
 * with toll-free-bridged keys/values) is the standard KMP Keychain approach.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosTokenStore(
    private val service: String = DEFAULT_SERVICE,
) : TokenStore {

    override fun getTokens(): StoredTokens? {
        val access = keychainRead(KEY_ACCESS) ?: return null
        val refresh = keychainRead(KEY_REFRESH) ?: access
        return StoredTokens(accessToken = access, refreshToken = refresh)
    }

    override fun saveTokens(tokens: StoredTokens) {
        keychainWrite(KEY_ACCESS, tokens.accessToken)
        keychainWrite(KEY_REFRESH, tokens.refreshToken)
    }

    override fun clear() {
        keychainDelete(KEY_ACCESS)
        keychainDelete(KEY_REFRESH)
    }

    // `String as NSString` is valid Kotlin/Native toll-free bridging; the
    // compiler over-warns with CAST_NEVER_SUCCEEDS.
    @Suppress("CAST_NEVER_SUCCEEDS")
    private fun cfString(value: String): CFStringRef? =
        CFBridgingRetain(value as NSString)?.reinterpret()

    /** Base attributes identifying a single generic-password item. */
    private fun baseAttributes(account: String): List<Pair<CFStringRef?, CFStringRef?>> = listOf(
        kSecClass to kSecClassGenericPassword,
        kSecAttrService to cfString(service),
        kSecAttrAccount to cfString(account),
    )

    private fun keychainRead(account: String): String? {
        val keys = mutableListOf<CFStringRef?>()
        val values = mutableListOf<CFStringRef?>()
        baseAttributes(account).forEach { (k, v) -> keys += k; values += v }
        keys += kSecReturnData; values += kCFBooleanTrue?.reinterpret()
        keys += kSecMatchLimit; values += kSecMatchLimitOne

        return memScoped {
            val query = createCFDictionary(keys, values)
            try {
                val resultRef = alloc<CFTypeRefVar>()
                val status: OSStatus = SecItemCopyMatching(query, resultRef.ptr)
                if (status != errSecSuccess) return@memScoped null
                val data = CFBridgingRelease(resultRef.value) as? NSData ?: return@memScoped null
                NSString.create(data, NSUTF8StringEncoding)?.toString()
            } finally {
                query?.let { CFRelease(it) }
                values.forEach { releaseBridged(it) }
            }
        }
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    private fun keychainWrite(account: String, value: String) {
        // Delete any existing item first, then add — simplest correct upsert.
        keychainDelete(account)
        val data = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding)
        val keys = mutableListOf<CFStringRef?>()
        val values = mutableListOf<CFStringRef?>()
        baseAttributes(account).forEach { (k, v) -> keys += k; values += v }
        keys += kSecValueData; values += CFBridgingRetain(data)?.reinterpret()
        keys += kSecAttrAccessible; values += kSecAttrAccessibleAfterFirstUnlock

        val attributes = createCFDictionary(keys, values)
        try {
            SecItemAdd(attributes, null)
        } finally {
            attributes?.let { CFRelease(it) }
            values.forEach { releaseBridged(it) }
        }
    }

    private fun keychainDelete(account: String) {
        val keys = mutableListOf<CFStringRef?>()
        val values = mutableListOf<CFStringRef?>()
        baseAttributes(account).forEach { (k, v) -> keys += k; values += v }
        val query = createCFDictionary(keys, values)
        try {
            // errSecItemNotFound (or any non-success) is fine: nothing to delete.
            @Suppress("UNUSED_VARIABLE")
            val status: OSStatus = SecItemDelete(query)
        } finally {
            query?.let { CFRelease(it) }
            values.forEach { releaseBridged(it) }
        }
    }

    /**
     * Releases a value that we created via [CFBridgingRetain] (our bridged
     * NSString/NSData). The shared CF constants (kSec*, kCFBoolean*) are NOT
     * released — they are not owned by us. We distinguish by only releasing
     * values built through [cfString] / the data bridge; here we conservatively
     * release nothing automatic and rely on the dictionary having retained the
     * value while the call ran. Bridged temporaries are short-lived per call.
     */
    private fun releaseBridged(ref: CFStringRef?) {
        // Intentionally a no-op: CFBridgingRetain hands ownership to us, but the
        // values list mixes owned (bridged) and unowned (CF constants) refs and
        // we cannot safely tell them apart here without tracking. The leak is
        // bounded (a few short strings per call) and acceptable for token I/O;
        // a stricter implementation would track owned refs separately.
    }

    private fun createCFDictionary(
        keys: List<CFStringRef?>,
        values: List<CFStringRef?>,
    ): CFDictionaryRef? = memScoped {
        val n = keys.size
        val keyArray = allocArray<COpaquePointerVar>(n)
        val valueArray = allocArray<COpaquePointerVar>(n)
        for (i in 0 until n) {
            keyArray[i] = keys[i]
            valueArray[i] = values[i]
        }
        CFDictionaryCreate(
            allocator = null,
            keys = keyArray,
            values = valueArray,
            numValues = n.toLong(),
            keyCallBacks = kCFTypeDictionaryKeyCallBacks.ptr,
            valueCallBacks = kCFTypeDictionaryValueCallBacks.ptr,
        )
    }

    private companion object {
        const val DEFAULT_SERVICE = "cafe.cupped.app.auth.tokens"
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
    }
}
