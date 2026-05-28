package cafe.cupped.app.db

import android.content.Context
import cafe.cupped.app.platform.KeystoreSecretStore
import java.security.SecureRandom

/**
 * Android [DatabaseKeyProvider]. The SQLCipher passphrase is a random 256-bit
 * secret persisted via [KeystoreSecretStore], which performs AES-256-GCM
 * envelope encryption with a key held in the Android Keystore. See
 * architecture §12.
 */
class AndroidDatabaseKeyProvider(context: Context) : DatabaseKeyProvider {

    private val secrets = KeystoreSecretStore(context)

    private val keyLock = Any()

    override fun getOrCreateKey(): String {
        secrets.get(KEY_DB_PASSPHRASE)?.let { return it }
        // The key is used immediately to open SQLCipher, so persistence must be
        // synchronous: an async write not yet flushed before a crash (or a
        // concurrent generation race) would leave the DB unopenable next launch.
        // Lock + re-check + synchronous commit (KeystoreSecretStore.put uses
        // commit()) closes both windows.
        return synchronized(keyLock) {
            secrets.get(KEY_DB_PASSPHRASE) ?: generateKey().also { generated ->
                secrets.put(KEY_DB_PASSPHRASE, generated)
            }
        }
    }

    override fun rotateKey() {
        synchronized(keyLock) {
            secrets.remove(KEY_DB_PASSPHRASE)
        }
    }

    private fun generateKey(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return bytes.toHexKey()
    }

    private companion object {
        const val KEY_DB_PASSPHRASE = "db_passphrase"
    }
}
