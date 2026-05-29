package cafe.cupped.app.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import cafe.cupped.app.isDebug
import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.sqliter.JournalMode
import io.github.aakira.napier.Napier

/**
 * iOS [DatabaseDriverFactory] using SQLDelight's native SQLiter driver.
 *
 * ## Fail-CLOSED on unencrypted DB (architecture §12)
 * SQLCipher-on-iOS is NOT yet wired (blocker: CocoaPods, see TODO below). Until
 * it is, opening the DB would write **plaintext** user data. A RELEASE build
 * therefore REFUSES to open an unencrypted DB (throws). Only a DEBUG build is
 * allowed to fall through to the unencrypted path, gated on
 * [isDebug] (`Platform.isDebugBinary` on Kotlin/Native). No release iOS build
 * may silently persist plaintext user data.
 *
 * ## Driver configuration (architecture §10/§S6)
 * The SQLiter [DatabaseConfiguration] requests:
 *  - `JournalMode.WAL` — single writer + concurrent readers.
 *  - a non-zero `busyTimeout` so a brief writer lock doesn't immediately error.
 *  - an explicit reader-pool size (`maxReaderConnections`) matching the
 *    "single writer + reader pool" intent.
 *
 * ## TODO(SQLCipher iOS) — exact remaining steps to wire encryption
 *  1. Add the Kotlin CocoaPods plugin (`kotlin("native.cocoapods")`) to
 *     shared/build.gradle.kts and declare the SQLCipher pod:
 *       cocoapods {
 *         pod("SQLCipher") { /* version */ }
 *       }
 *     plus a Podfile in iosApp/ and run `pod install`.
 *  2. Set `linkSqlite = false` on the iOS targets so SQLiter links against the
 *     SQLCipher pod's libsqlite symbols instead of the system SQLite.
 *  3. Pass the key by setting `encryptionConfig = Encryption(key = rawKeyFormat(
 *     keyProvider.getOrCreateKey()))` on the [DatabaseConfiguration] below
 *     (SQLiter issues `PRAGMA key = '<key>'` when SQLCipher is the linked
 *     engine). Use [rawKeyFormat] so the key matches Android's raw-key format.
 *  4. Delete the [isDebug] fail-closed gate once (3) is verified on-device.
 */
class IosDatabaseDriverFactory(
    private val keyProvider: DatabaseKeyProvider,
) : DatabaseDriverFactory {

    override fun createDriver(): SqlDriver {
        // Exercise the keystore path (and validate the key format) regardless of
        // whether encryption is wired yet, so the Keychain round-trip is tested.
        val key = rawKeyFormat(keyProvider.getOrCreateKey())

        if (!isDebug) {
            // RELEASE: refuse to open an unencrypted DB. (Remove once SQLCipher
            // is wired per the TODO above and `encryptionConfig` is set.)
            throw IllegalStateException(
                "Refusing to open an UNENCRYPTED SQLite DB in a release build. " +
                    "SQLCipher-on-iOS is not wired yet (see IosDatabaseDriverFactory TODO). " +
                    "No plaintext user data may be persisted in release.",
            )
        }

        Napier.w(
            "iOS DB opening UNENCRYPTED (DEBUG only). SQLCipher pod not wired; " +
                "key derived but not applied. See IosDatabaseDriverFactory TODO.",
        )

        @Suppress("UNUSED_VARIABLE")
        val unusedUntilWired = key // referenced so the keystore call is not DCE'd

        return NativeSqliteDriver(
            schema = CuppedDatabase.Schema,
            name = CUPPED_DB_NAME,
            maxReaderConnections = DB_MAX_READERS,
            onConfiguration = { config: DatabaseConfiguration ->
                config.copy(
                    journalMode = JournalMode.WAL,
                    extendedConfig = config.extendedConfig.copy(
                        busyTimeout = DB_BUSY_TIMEOUT_MS,
                    ),
                    // TODO(SQLCipher iOS): encryptionConfig =
                    //   config.encryptionConfig.copy(key = key)
                )
            },
        )
    }

    private companion object {
        // Single writer + small reader pool under WAL (architecture §10/§S6).
        const val DB_MAX_READERS = 4
        // Wait briefly on a writer lock instead of failing immediately.
        const val DB_BUSY_TIMEOUT_MS = 3_000
    }
}
