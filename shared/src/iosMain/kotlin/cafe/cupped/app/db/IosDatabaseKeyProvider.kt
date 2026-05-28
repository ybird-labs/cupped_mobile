package cafe.cupped.app.db

import cafe.cupped.app.platform.IosKeychain
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.addressOf
import platform.Security.SecRandomCopyBytes
import platform.Security.kSecRandomDefault

/**
 * iOS [DatabaseKeyProvider]. The SQLCipher passphrase is a random 256-bit
 * secret persisted in the Keychain (architecture §12).
 */
@OptIn(ExperimentalForeignApi::class)
class IosDatabaseKeyProvider(
    service: String = DEFAULT_SERVICE,
) : DatabaseKeyProvider {

    private val keychain = IosKeychain(service)

    override fun getOrCreateKey(): String {
        keychain.read(KEY_DB_PASSPHRASE)?.let { return it }
        val generated = generateKey()
        keychain.write(KEY_DB_PASSPHRASE, generated)
        return generated
    }

    override fun rotateKey() {
        keychain.delete(KEY_DB_PASSPHRASE)
    }

    private fun generateKey(): String {
        val bytes = ByteArray(32)
        bytes.usePinned { pinned ->
            SecRandomCopyBytes(kSecRandomDefault, bytes.size.toULong(), pinned.addressOf(0))
        }
        return bytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
    }

    private companion object {
        const val DEFAULT_SERVICE = "cafe.cupped.app.db.key"
        const val KEY_DB_PASSPHRASE = "db_passphrase"
    }
}
