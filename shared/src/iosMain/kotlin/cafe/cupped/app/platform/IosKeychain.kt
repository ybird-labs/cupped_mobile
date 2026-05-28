package cafe.cupped.app.platform

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
 * Minimal Keychain wrapper over generic-password items, shared by the token
 * store and the database key provider. Items use
 * `kSecAttrAccessibleAfterFirstUnlock`.
 *
 * NOTE (unverified linking): this compiles to Kotlin/Native but the Security
 * framework calls have not been exercised on a device/simulator in this
 * environment. The CoreFoundation interop (CFDictionaryCreate with toll-free
 * bridged keys/values) is the standard KMP Keychain approach.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosKeychain(private val service: String) {

    fun read(account: String): String? {
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
            }
        }
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    fun write(account: String, value: String) {
        delete(account)
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
        }
    }

    fun delete(account: String) {
        val keys = mutableListOf<CFStringRef?>()
        val values = mutableListOf<CFStringRef?>()
        baseAttributes(account).forEach { (k, v) -> keys += k; values += v }
        val query = createCFDictionary(keys, values)
        try {
            @Suppress("UNUSED_VARIABLE")
            val status: OSStatus = SecItemDelete(query)
        } finally {
            query?.let { CFRelease(it) }
        }
    }

    // `String as NSString` is valid Kotlin/Native toll-free bridging; the
    // compiler over-warns with CAST_NEVER_SUCCEEDS.
    @Suppress("CAST_NEVER_SUCCEEDS")
    private fun cfString(value: String): CFStringRef? =
        CFBridgingRetain(value as NSString)?.reinterpret()

    private fun baseAttributes(account: String): List<Pair<CFStringRef?, CFStringRef?>> = listOf(
        kSecClass to kSecClassGenericPassword,
        kSecAttrService to cfString(service),
        kSecAttrAccount to cfString(account),
    )

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
}
