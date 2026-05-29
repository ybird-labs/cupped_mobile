package cafe.cupped.app.db

import app.cash.sqldelight.db.SqlDriver

/**
 * Creates the platform [SqlDriver] for [CuppedDatabase], opening the on-disk
 * database with SQLCipher encryption keyed from the platform keystore
 * (architecture §12).
 *
 * Implementations:
 *  - Android: SupportSQLiteOpenHelper backed by `net.zetetic:sqlcipher-android`,
 *    PRAGMA key set from the Android Keystore.
 *  - iOS: SQLDelight native SQLiter driver. SQLCipher is linked via a static
 *    CocoaPods pod (`linkSqlite=false`), key from the Keychain.
 *    See PHASE2 TODO in the iOS actual: the CocoaPods pod is NOT yet wired in
 *    this worktree, so the iOS driver currently opens an UNENCRYPTED database.
 */
interface DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

/** Logical name of the on-disk database file (per-user wipe/rekey on logout). */
const val CUPPED_DB_NAME: String = "cupped.db"
