package cafe.cupped.app.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
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
        val passphrase = rawKeyFormat(keyProvider.getOrCreateKey()).encodeToByteArray()
        val factory = SupportOpenHelperFactory(passphrase)
        return AndroidSqliteDriver(
            schema = CuppedDatabase.Schema,
            context = context.applicationContext,
            name = CUPPED_DB_NAME,
            factory = factory,
        )
    }
}
