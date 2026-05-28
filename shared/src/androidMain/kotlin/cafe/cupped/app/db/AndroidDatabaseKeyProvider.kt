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

    override fun getOrCreateKey(): String {
        prefs.getString(KEY_DB_PASSPHRASE, null)?.let { return it }
        val generated = generateKey()
        prefs.edit().putString(KEY_DB_PASSPHRASE, generated).apply()
        return generated
    }

    override fun rotateKey() {
        prefs.edit().remove(KEY_DB_PASSPHRASE).apply()
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
