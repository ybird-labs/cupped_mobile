package cafe.cupped.app.db

import cafe.cupped.app.platform.KeystoreSecretStore
import java.security.SecureRandom

/**
 * Android [DatabaseKeyProvider]. The SQLCipher passphrase is a random 256-bit
 * secret persisted via the shared [KeystoreSecretStore] (AES-256-GCM envelope
 * encryption with an Android Keystore key). The store is injected so it is a
 * single instance shared with the token store. See architecture §12.
 *
 * Recovery contract: [getOrCreateKey] PROPAGATES
 * [cafe.cupped.app.platform.SecretUndecryptableException] when a stored
 * passphrase exists but can't be decrypted (Keystore key lost/invalidated). It
 * deliberately does NOT silently re-key, because the existing SQLCipher DB file
 * is still encrypted under the lost key — re-keying in place would run SQLCipher
 * with a new key against the old file and could drop unsynced data. The caller
 * (AndroidDatabaseDriverFactory) handles recovery: delete the DB file, then
 * [rotateKey] + reopen, so the local cache is wiped and re-synced from the server.
 */
class AndroidDatabaseKeyProvider(
    private val secrets: KeystoreSecretStore,
) : DatabaseKeyProvider {

    private val keyLock = Any()

    // The key is used immediately to open SQLCipher, so persistence must be
    // synchronous: an async write not yet flushed before a crash (or a concurrent
    // generation race) would leave the DB unopenable next launch. Lock + re-check
    // + synchronous commit (KeystoreSecretStore.put uses commit()) closes both.
    // SecretUndecryptableException propagates (see class KDoc).
    override fun getOrCreateKey(): String = synchronized(keyLock) {
        secrets.get(KEY_DB_PASSPHRASE) ?: generateKey().also { generated ->
            secrets.put(KEY_DB_PASSPHRASE, generated)
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
