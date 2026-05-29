package cafe.cupped.app.db

import cafe.cupped.app.platform.KeystoreSecretStore
import cafe.cupped.app.platform.SecretUndecryptableException
import io.github.aakira.napier.Napier
import java.security.SecureRandom

/**
 * Android [DatabaseKeyProvider]. The SQLCipher passphrase is a random 256-bit
 * secret persisted via the shared [KeystoreSecretStore] (AES-256-GCM envelope
 * encryption with an Android Keystore key). The store is injected so it is a
 * single instance shared with the token store. See architecture §12.
 */
class AndroidDatabaseKeyProvider(
    private val secrets: KeystoreSecretStore,
) : DatabaseKeyProvider {

    private val keyLock = Any()

    // The key is used immediately to open SQLCipher, so persistence must be
    // synchronous: an async write not yet flushed before a crash (or a concurrent
    // generation race) would leave the DB unopenable next launch. Lock + re-check
    // + synchronous commit (KeystoreSecretStore.put uses commit()) closes both.
    override fun getOrCreateKey(): String = synchronized(keyLock) {
        val existing = try {
            secrets.get(KEY_DB_PASSPHRASE)
        } catch (e: SecretUndecryptableException) {
            // The stored passphrase exists but can't be decrypted (Keystore key
            // invalidated/rotated, or blob corrupt). The old passphrase is
            // unrecoverable, so any existing SQLCipher DB encrypted under it is
            // permanently un-openable. Re-key INTENTIONALLY and loudly — the local
            // DB must be wiped + re-synced from the server (the DB-open recovery
            // path handles deletion). This must never be silent (would otherwise
            // re-key over an unopenable DB and look like a fresh install).
            Napier.w(
                "DB passphrase unrecoverable; re-keying. Existing local DB cannot be " +
                    "decrypted and must be wiped + re-synced from the server.",
                e,
            )
            secrets.remove(KEY_DB_PASSPHRASE)
            null
        }
        existing ?: generateKey().also { generated ->
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
