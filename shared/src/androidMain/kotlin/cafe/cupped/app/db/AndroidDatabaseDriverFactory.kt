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
        val passphrase = keyProvider.getOrCreateKey().encodeToByteArray()
        val factory = SupportOpenHelperFactory(passphrase)
        return AndroidSqliteDriver(
            schema = CuppedDatabase.Schema,
            context = context.applicationContext,
            name = CUPPED_DB_NAME,
            factory = factory,
        )
    }
}
