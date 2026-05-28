package cafe.cupped.app.db

/**
 * Supplies the SQLCipher passphrase for the local database (architecture §12).
 *
 * The passphrase is a random 256-bit secret generated once and stored in the
 * platform keystore (Android Keystore-backed EncryptedSharedPreferences /
 * iOS Keychain). It is NOT the user's credential; it is a device-local DB key.
 *
 * On logout / account switch the database is wiped and re-keyed (architecture
 * §11): callers delete the DB file and call [rotateKey] so the next login's
 * full resync writes to a fresh encrypted database.
 */
interface DatabaseKeyProvider {
    /** Returns the existing key, generating and persisting one on first use. */
    fun getOrCreateKey(): String

    /** Discards the stored key so a new one is generated next time (logout/rekey). */
    fun rotateKey()
}
