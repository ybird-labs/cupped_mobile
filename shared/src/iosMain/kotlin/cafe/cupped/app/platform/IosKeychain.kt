package cafe.cupped.app.platform

import io.github.aakira.napier.Napier
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.COpaquePointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
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
import platform.Security.SecItemUpdate
import platform.Security.errSecItemNotFound
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
 * String <-> Foundation bridging: a Kotlin `String` is NOT an `NSString` on
 * Kotlin/Native (the old `value as NSString` cast threw `ClassCastException`
 * at runtime). We bridge explicitly via `NSString.create(string = ...)` and
 * `dataUsingEncoding(NSUTF8StringEncoding)` for values, and
 * `CFBridgingRetain(NSString)` for CFString attribute keys/values.
 *
 * CoreFoundation lifetime: `CFDictionaryCreate` is called with the standard
 * key/value callbacks (`kCFTypeDictionaryKeyCallBacks` /
 * `kCFTypeDictionaryValueCallBacks`), which RETAIN every key and value the
 * dictionary stores. The dictionary therefore owns its own +1 reference to each
 * CFString/CFData we hand it, so the local `CFBridgingRetain` references we
 * created are released right after the dictionary is built (see
 * [createCFDictionary]) and the dictionary keeps the values alive for the
 * duration of the Sec* call.
 *
 * NOTE (device-only): this compiles for Kotlin/Native but the Security
 * framework calls have not been exercised on a device/simulator in this
 * environment.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosKeychain(private val service: String) {

    /**
     * Returns the stored UTF-8 string for [account], or null when no item
     * exists (`errSecItemNotFound`). Any other non-success OSStatus is logged
     * (so a real keychain error is not silently mistaken for "signed out").
     */
    fun read(account: String): String? {
        val refs = mutableListOf<CFStringRef?>()
        val keys = mutableListOf<CFStringRef?>()
        val values = mutableListOf<CFStringRef?>()
        baseAttributes(account, refs).forEach { (k, v) -> keys += k; values += v }
        keys += kSecReturnData; values += kCFBooleanTrue?.reinterpret()
        keys += kSecMatchLimit; values += kSecMatchLimitOne

        val query = createCFDictionary(keys, values)
        releaseRetained(refs)
        return memScoped {
            try {
                val resultRef = alloc<CFTypeRefVar>()
                val status: OSStatus = SecItemCopyMatching(query, resultRef.ptr)
                when (status) {
                    errSecSuccess -> {
                        // CFBridgingRelease transfers ownership of the copied
                        // result to ARC/Kotlin, balancing the +1 from the copy.
                        val data = CFBridgingRelease(resultRef.value) as? NSData
                            ?: return@memScoped null
                        NSString.create(data, NSUTF8StringEncoding)?.toString()
                    }
                    errSecItemNotFound -> null
                    else -> {
                        Napier.e("Keychain read failed for '$account': OSStatus=$status")
                        null
                    }
                }
            } finally {
                query?.let { CFRelease(it) }
            }
        }
    }

    /**
     * Atomic find-or-update write. Tries `SecItemUpdate` against the identity
     * query first and falls back to `SecItemAdd` only when the item does not yet
     * exist (`errSecItemNotFound`). This avoids the delete-then-add race window
     * (where a concurrent reader could observe a missing item between the two
     * calls) and never deletes existing data on a transient failure.
     */
    fun write(account: String, value: String) {
        val data: NSData = NSString.create(string = value)
            .dataUsingEncoding(NSUTF8StringEncoding)
            ?: run {
                Napier.e("Keychain write for '$account': failed to UTF-8 encode value")
                return
            }
        val dataRef = CFBridgingRetain(data)
        try {
            // Identity query: class + service + account (no value).
            val queryRefs = mutableListOf<CFStringRef?>()
            val qKeys = mutableListOf<CFStringRef?>()
            val qValues = mutableListOf<CFStringRef?>()
            baseAttributes(account, queryRefs).forEach { (k, v) -> qKeys += k; qValues += v }
            val query = createCFDictionary(qKeys, qValues)

            // Update payload: only the new value bytes.
            val updateDict = createCFDictionary(
                listOf(kSecValueData),
                listOf(dataRef?.reinterpret()),
            )
            releaseRetained(queryRefs)

            try {
                when (val updateStatus = SecItemUpdate(query, updateDict)) {
                    errSecSuccess -> Unit // updated in place
                    errSecItemNotFound -> {
                        // No existing item — add a fresh one with full attributes.
                        val addRefs = mutableListOf<CFStringRef?>()
                        val aKeys = mutableListOf<CFStringRef?>()
                        val aValues = mutableListOf<CFStringRef?>()
                        baseAttributes(account, addRefs).forEach { (k, v) -> aKeys += k; aValues += v }
                        aKeys += kSecValueData; aValues += dataRef?.reinterpret()
                        aKeys += kSecAttrAccessible; aValues += kSecAttrAccessibleAfterFirstUnlock
                        val addAttrs = createCFDictionary(aKeys, aValues)
                        releaseRetained(addRefs)
                        try {
                            val addStatus = SecItemAdd(addAttrs, null)
                            if (addStatus != errSecSuccess) {
                                Napier.e("Keychain add failed for '$account': OSStatus=$addStatus")
                            }
                        } finally {
                            addAttrs?.let { CFRelease(it) }
                        }
                    }
                    else -> Napier.e("Keychain update failed for '$account': OSStatus=$updateStatus")
                }
            } finally {
                query?.let { CFRelease(it) }
                updateDict?.let { CFRelease(it) }
            }
        } finally {
            dataRef?.let { CFRelease(it) }
        }
    }

    fun delete(account: String) {
        val refs = mutableListOf<CFStringRef?>()
        val keys = mutableListOf<CFStringRef?>()
        val values = mutableListOf<CFStringRef?>()
        baseAttributes(account, refs).forEach { (k, v) -> keys += k; values += v }
        val query = createCFDictionary(keys, values)
        releaseRetained(refs)
        try {
            val status: OSStatus = SecItemDelete(query)
            if (status != errSecSuccess && status != errSecItemNotFound) {
                Napier.e("Keychain delete failed for '$account': OSStatus=$status")
            }
        } finally {
            query?.let { CFRelease(it) }
        }
    }

    /**
     * Bridges a Kotlin String to a retained CFString. The returned ref has a +1
     * retain count owned by the caller; it is appended to [retained] so the
     * caller can release it after [createCFDictionary] has retained it itself.
     */
    private fun cfString(value: String, retained: MutableList<CFStringRef?>): CFStringRef? {
        val ref: CFStringRef? = CFBridgingRetain(NSString.create(string = value))?.reinterpret()
        retained += ref
        return ref
    }

    private fun releaseRetained(retained: List<CFStringRef?>) {
        retained.forEach { it?.let { ref -> CFRelease(ref) } }
    }

    private fun baseAttributes(
        account: String,
        retained: MutableList<CFStringRef?>,
    ): List<Pair<CFStringRef?, CFStringRef?>> = listOf(
        kSecClass to kSecClassGenericPassword,
        kSecAttrService to cfString(service, retained),
        kSecAttrAccount to cfString(account, retained),
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
        // CFType key/value callbacks => the dictionary retains each key/value,
        // so our local CFBridgingRetain references can be released afterwards.
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
