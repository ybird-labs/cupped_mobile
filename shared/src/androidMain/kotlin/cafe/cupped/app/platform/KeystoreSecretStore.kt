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
 *    only when the entry is ABSENT; it THROWS [SecretUndecryptableException] when
 *    an entry exists but cannot be decoded/decrypted (corrupt blob, or the
 *    keystore key was invalidated/rotated — wrapping the underlying cause, e.g.
 *    [KeyPermanentlyInvalidatedException]). On permanent invalidation it also
 *    calls [deleteSecretKey] so a later [put] can re-key. Callers MUST catch
 *    [SecretUndecryptableException] and recover (sign out / wipe + re-sync); the
 *    prior plaintext is unrecoverable.
 *  - [put] self-heals: if encryption hits [KeyPermanentlyInvalidatedException]
 *    (the key died since it was created), it deletes the dead alias and retries
 *    once with a fresh key, so the store can never get permanently stuck unwritable.
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
            val encoded = try {
                encryptToBase64(plaintext)
            } catch (e: KeyPermanentlyInvalidatedException) {
                // The keystore key died since it was created (biometric re-enroll,
                // device-credential change, etc.). Anything stored under it is
                // already unrecoverable, so regenerate and retry ONCE — otherwise a
                // single invalidation would leave the store permanently unwritable
                // (every put() would re-throw uncaught). This is the self-heal path
                // that account/DB recovery relies on. A second failure propagates
                // (genuinely broken keystore) rather than looping.
                Napier.w("Keystore key invalidated during put('$key'); re-keying and retrying once", e)
                deleteSecretKey()
                encryptToBase64(plaintext)
            }
            check(prefs.edit().putString(key, encoded).commit()) {
                "Failed to persist encrypted secret for '$key' to $PREFS_FILE_NAME"
            }
        }
    }

    /** Encrypts [plaintext] under the keystore key and returns base64(iv ++ ciphertext). */
    private fun encryptToBase64(plaintext: String): String {
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val blob = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, blob, 0, iv.size)
        System.arraycopy(ciphertext, 0, blob, iv.size, ciphertext.size)
        return Base64.encodeToString(blob, Base64.NO_WRAP)
    }

    /**
     * Returns the decrypted UTF-8 string stored under [key], or null when the
     * entry is genuinely ABSENT. Throws [SecretUndecryptableException] when an
     * entry EXISTS but cannot be decrypted (Keystore key invalidated/rotated, or
     * the stored blob is corrupt). Callers MUST distinguish this from absent,
     * because a present-but-undecryptable secret means the prior plaintext is
     * unrecoverable — silently treating it as "no value" would, for the DB
     * passphrase, re-key over an existing un-openable database.
     */
    fun get(key: String): String? {
        synchronized(lock) {
            val encoded = prefs.getString(key, null) ?: return null
            return try {
                // Decode INSIDE the try so a corrupt blob's IllegalArgumentException
                // (and the too-short check) are wrapped as SecretUndecryptableException
                // — callers only recover from that, so a raw throw here would crash.
                val blob = Base64.decode(encoded, Base64.NO_WRAP)
                check(blob.size > GCM_IV_LENGTH) {
                    "stored blob too short to contain IV + ciphertext"
                }
                val iv = blob.copyOfRange(0, GCM_IV_LENGTH)
                val ciphertext = blob.copyOfRange(GCM_IV_LENGTH, blob.size)
                val secretKey = getOrCreateSecretKey()
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
                String(cipher.doFinal(ciphertext), Charsets.UTF_8)
            } catch (e: KeyPermanentlyInvalidatedException) {
                // The Keystore key is permanently dead; getOrCreateSecretKey would
                // keep returning it and every future get/put would fail. Delete the
                // alias so the next put() regenerates a fresh key. Everything stored
                // under the old key is now unrecoverable.
                deleteSecretKey()
                throw SecretUndecryptableException(key, e)
            } catch (e: Exception) {
                throw SecretUndecryptableException(key, e)
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

    /**
     * Deletes the Keystore key alias so the next [getOrCreateSecretKey] generates
     * a fresh key. Called when the existing key is permanently invalidated.
     *
     * Intentionally best-effort (failure is logged, not rethrown): its two callers
     * stay correct without it succeeding. [get] throws [SecretUndecryptableException]
     * regardless of whether the alias was cleared (the stored value is unrecoverable
     * either way), and [put] does NOT trust this to have worked — if the alias is
     * still present and still invalid, [put]'s retry re-hits
     * [KeyPermanentlyInvalidatedException] and propagates it (fail-loud) rather than
     * silently persisting under a dead key. So a swallowed delete can never leave
     * the store silently corrupt — only loudly broken on a genuinely unusable keystore.
     */
    private fun deleteSecretKey() {
        try {
            KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.deleteEntry(KEY_ALIAS)
        } catch (e: Exception) {
            Napier.e("Failed to delete invalidated Keystore key '$KEY_ALIAS'", e)
        }
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

/**
 * Thrown by [KeystoreSecretStore.get] when an entry EXISTS but cannot be
 * decrypted — Keystore key invalidated/rotated, or the stored blob is corrupt.
 * Distinct from a genuinely-absent entry (which returns null): a
 * present-but-undecryptable secret means the prior plaintext is unrecoverable,
 * so callers must choose recovery (sign out / wipe + re-sync) rather than
 * silently re-keying.
 */
class SecretUndecryptableException(key: String, cause: Throwable?) :
    Exception("Secret '$key' exists but could not be decrypted", cause)
