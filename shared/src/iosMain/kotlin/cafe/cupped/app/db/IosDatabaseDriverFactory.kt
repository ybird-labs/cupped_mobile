package cafe.cupped.app.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

/**
 * iOS [DatabaseDriverFactory] using SQLDelight's native SQLiter driver.
 *
 * ⚠️ PHASE 2 TODO — SQLCipher-on-iOS is NOT yet wired (blocker: CocoaPods).
 * Per architecture §12 and the research (Touchlab approach), iOS encryption
 * requires:
 *   1. The Kotlin CocoaPods plugin (`kotlin("native.cocoapods")`) in
 *      shared/build.gradle.kts, declaring the SQLCipher pod, and a Podfile in
 *      iosApp/ that runs `pod install`.
 *   2. `linkSqlite = false` on the iOS targets so SQLiter links against the
 *      SQLCipher pod's libsqlite symbols instead of the system SQLite.
 *   3. Passing the passphrase to SQLiter via `DatabaseConfiguration.Extended`
 *      `key = keyProvider.getOrCreateKey()` (SQLiter exposes a `key` knob that
 *      issues `PRAGMA key` when SQLCipher is the linked engine).
 *
 * This worktree has NO CocoaPods integration (no Podfile, no cocoapods{} block),
 * and adding it needs CocoaPods installed + Xcode project changes that cannot be
 * verified here. Until that lands, this driver opens an UNENCRYPTED database on
 * iOS. The [keyProvider] is already injected and ready so only the build wiring
 * + the SQLiter `key`/`linkSqlite=false` config remain.
 */
class IosDatabaseDriverFactory(
    private val keyProvider: DatabaseKeyProvider,
) : DatabaseDriverFactory {

    override fun createDriver(): SqlDriver {
        // TODO(SQLCipher iOS): once the CocoaPods SQLCipher pod is wired with
        //  linkSqlite=false, construct NativeSqliteDriver with a
        //  DatabaseConfiguration whose `key` is keyProvider.getOrCreateKey().
        //  Until then the key is read (to keep the keystore path exercised) but
        //  the DB is not yet encrypted.
        keyProvider.getOrCreateKey()
        return NativeSqliteDriver(
            schema = CuppedDatabase.Schema,
            name = CUPPED_DB_NAME,
        )
    }
}
