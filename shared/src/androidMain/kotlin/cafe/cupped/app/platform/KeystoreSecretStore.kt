package cafe.cupped.app.platform

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import io.github.aakira.napier.Napier
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android secret store backed directly by the [AndroidKeyStore][ANDROID_KEYSTORE].
 * Mirrors the role of iOS `platform/IosKeychain.kt`: a minimal, shared,
 * platform-keystore-backed secure store used by the token store and the
 * database key provider.
 *
 * Design (envelope encryption):
 *  - A single AES-256 [SecretKey] lives non-exportably in the AndroidKeyStore
 *    under [KEY_ALIAS]. It is lazily get-or-created with NO user-authentication
 *    requirement, so secrets stay readable without a biometric/PIN prompt (the
 *    SQLCipher passphrase and auth tokens must be usable on a cold start).
 *  - [put] encrypts the plaintext with `AES/GCM/NoPadding`, letting the Cipher
 *    generate a fresh 12-byte IV, and persists `base64(iv ++ ciphertext)` into a
 *    PLAIN [SharedPreferences] file ([PREFS_FILE_NAME]). The plain prefs only
 *    ever hold ciphertext + IV; the encryption key never leaves the keystore.
 *  - [get] reads the base64 blob, splits off the leading 12-byte IV, and
 *    decrypts with `GCMParameterSpec(128, iv)` (128-bit auth tag). Returns null
 *    when the entry is absent, and logs + returns null on any decrypt failure or
 *    keystore invalidation rather than crashing.
 *
 * Thread-safety: all keystore + prefs mutations are guarded by [lock]; writes use
 * synchronous [SharedPreferences.Editor.commit] (not apply) so a secret is
 * durable before it is used to open SQLCipher.
 *
 * NOTE (device-only): this compiles for Android but the AndroidKeyStore /
 * Cipher operations have not been exercised on a device/emulator in this
 * environment.
 */
class KeystoreSecretStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)

    private val lock = Any()

    /** Persists [plaintext] encrypted under [key]. Synchronous + thread-safe. */
    fun put(key: String, plaintext: String) {
        synchronized(lock) {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            val blob = ByteArray(iv.size + ciphertext.size)
            System.arraycopy(iv, 0, blob, 0, iv.size)
            System.arraycopy(ciphertext, 0, blob, iv.size, ciphertext.size)
            val encoded = Base64.encodeToString(blob, Base64.NO_WRAP)
            check(prefs.edit().putString(key, encoded).commit()) {
                "Failed to persist encrypted secret for '$key' to $PREFS_FILE_NAME"
            }
        }
    }

    /**
     * Returns the decrypted UTF-8 string stored under [key], or null when the
     * entry is absent or cannot be decrypted (logged, never thrown).
     */
    fun get(key: String): String? {
        synchronized(lock) {
            val encoded = prefs.getString(key, null) ?: return null
            return try {
                val blob = Base64.decode(encoded, Base64.NO_WRAP)
                if (blob.size <= GCM_IV_LENGTH) {
                    Napier.e("Secret '$key' is too short to contain IV + ciphertext")
                    return null
                }
                val iv = blob.copyOfRange(0, GCM_IV_LENGTH)
                val ciphertext = blob.copyOfRange(GCM_IV_LENGTH, blob.size)
                val secretKey = getOrCreateSecretKey()
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
                String(cipher.doFinal(ciphertext), Charsets.UTF_8)
            } catch (e: KeyPermanentlyInvalidatedException) {
                Napier.e("Keystore key invalidated; cannot decrypt '$key'", e)
                null
            } catch (e: Exception) {
                Napier.e("Failed to decrypt secret '$key'", e)
                null
            }
        }
    }

    /** Removes the entry for [key]. Synchronous + thread-safe. */
    fun remove(key: String) {
        synchronized(lock) {
            check(prefs.edit().remove(key).commit()) {
                "Failed to remove secret '$key' from $PREFS_FILE_NAME"
            }
        }
    }

    /** Removes all stored entries. Synchronous + thread-safe. */
    fun clear() {
        synchronized(lock) {
            check(prefs.edit().clear().commit()) {
                "Failed to clear $PREFS_FILE_NAME"
            }
        }
    }

    /**
     * Lazily fetches the AES-256 keystore key, generating it on first use.
     * Must be called under [lock].
     */
    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let {
            return it.secretKey
        }
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE,
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            // No user-authentication requirement: the DB passphrase and auth
            // tokens must be readable on a cold start without a biometric prompt.
            .setUserAuthenticationRequired(false)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "cupped_secret_store_key"
        const val PREFS_FILE_NAME = "cupped_secure_store"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_SIZE_BITS = 256
        const val GCM_IV_LENGTH = 12
        const val GCM_TAG_BITS = 128
    }
}
