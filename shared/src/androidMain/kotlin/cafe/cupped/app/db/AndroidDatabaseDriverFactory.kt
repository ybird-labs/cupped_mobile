package cafe.cupped.app.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import cafe.cupped.app.platform.SecretUndecryptableException
import io.github.aakira.napier.Napier
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/**
 * Android [DatabaseDriverFactory] backed by SQLCipher
 * (`net.zetetic:sqlcipher-android`). The passphrase comes from the platform
 * keystore via [DatabaseKeyProvider] (architecture §12).
 */
class AndroidDatabaseDriverFactory(
    private val context: Context,
    private val keyProvider: DatabaseKeyProvider,
) : DatabaseDriverFactory {

    override fun createDriver(): SqlDriver {
        // Load the SQLCipher native libraries before opening the database.
        System.loadLibrary("sqlcipher")
        // RAW KEY format: pass the 256-bit CSPRNG key as SQLCipher raw-key
        // syntax `x'<hex>'` (NOT a passphrase). SQLCipher detects the `x'...'`
        // literal and uses the bytes directly with NO PBKDF2 derivation. iOS
        // must derive identically (it passes the same `x'<hex>'` via PRAGMA key)
        // so the two platforms' DBs are mutually readable (decision 2026-05-28).
        val rawKey = try {
            keyProvider.getOrCreateKey()
        } catch (e: SecretUndecryptableException) {
            // The stored DB passphrase is unrecoverable (Keystore key lost/
            // invalidated), so the existing SQLCipher DB file — encrypted under the
            // lost key — can no longer be opened. Wipe it and re-key here (NOT in the
            // key provider): the local DB is a cache that re-syncs from the server.
            // Delete the file FIRST so SQLCipher creates a fresh DB under the new key
            // instead of failing against a mismatched one. rotateKey() clears the
            // stale (undecryptable) entry so getOrCreateKey() then mints a fresh key.
            Napier.e("DB passphrase unrecoverable; wiping local DB and re-keying before reopen.", e)
            context.applicationContext.deleteDatabase(CUPPED_DB_NAME)
            keyProvider.rotateKey()
            keyProvider.getOrCreateKey()
        }
        val passphrase = rawKeyFormat(rawKey).encodeToByteArray()
        val factory = SupportOpenHelperFactory(passphrase)
        return AndroidSqliteDriver(
            schema = CuppedDatabase.Schema,
            context = context.applicationContext,
            name = CUPPED_DB_NAME,
            factory = factory,
        )
    }
}
