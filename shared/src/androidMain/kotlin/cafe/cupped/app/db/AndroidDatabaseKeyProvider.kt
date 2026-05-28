package cafe.cupped.app.db

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

/**
 * Android [DatabaseKeyProvider]. The SQLCipher passphrase is a random 256-bit
 * secret persisted in [EncryptedSharedPreferences] (master key in the Android
 * Keystore). See architecture §12.
 */
class AndroidDatabaseKeyProvider(context: Context) : DatabaseKeyProvider {

    private val prefs: SharedPreferences = run {
        val appContext = context.applicationContext
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            appContext,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private val keyLock = Any()

    override fun getOrCreateKey(): String {
        prefs.getString(KEY_DB_PASSPHRASE, null)?.let { return it }
        // The key is used immediately to open SQLCipher, so persistence must be
        // synchronous: an async apply() not yet flushed before a crash (or a
        // concurrent generation race) would leave the DB unopenable next launch.
        // Lock + re-check + commit() (which reports success) closes both windows.
        return synchronized(keyLock) {
            prefs.getString(KEY_DB_PASSPHRASE, null) ?: generateKey().also { generated ->
                check(prefs.edit().putString(KEY_DB_PASSPHRASE, generated).commit()) {
                    "Failed to persist SQLCipher passphrase to EncryptedSharedPreferences"
                }
            }
        }
    }

    override fun rotateKey() {
        synchronized(keyLock) {
            check(prefs.edit().remove(KEY_DB_PASSPHRASE).commit()) {
                "Failed to remove SQLCipher passphrase during rotateKey"
            }
        }
    }

    private fun generateKey(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return bytes.toHexKey()
    }

    private companion object {
        const val PREFS_FILE_NAME = "cupped_db_key"
        const val KEY_DB_PASSPHRASE = "db_passphrase"
    }
}
