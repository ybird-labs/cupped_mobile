package cafe.cupped.app.db

import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.withContext

/**
 * Builds the [CuppedDatabase] from a platform [DatabaseDriverFactory].
 *
 * ## Off-main-thread open (architecture §S4)
 * Opening the driver loads the native SQLite/SQLCipher library, may run schema
 * creation/migration, and (on Android) calls `System.loadLibrary`. None of that
 * may run on the main thread. [createAsync] performs the open on the injected
 * [transactionDispatcher] (an IO-style dispatcher in production, a
 * `TestDispatcher` in tests). [create] remains for callers that already control
 * their thread; the Koin provider uses [createAsync] / a lazy initializer so the
 * open never lands on the UI thread.
 *
 * ## Injected transaction dispatcher (architecture §10/§18, S5)
 * The [transactionDispatcher] is injectable (not hard-coded `Dispatchers.IO`)
 * so invariant tests can drive transactions under `kotlinx-coroutines-test`
 * virtual time. It is the dispatcher repositories / the sync engine wrap their
 * suspending transactions in, and is also the dispatcher the DB is opened on.
 */
class CuppedDatabaseFactory(
    private val driverFactory: DatabaseDriverFactory,
    val transactionDispatcher: TransactionDispatcher,
) {
    /** Opens the driver + database. Callers MUST NOT invoke this on the main thread. */
    fun create(): CuppedDatabase {
        val driver: SqlDriver = driverFactory.createDriver()
        return CuppedDatabase(driver)
    }

    /**
     * Opens the database on [transactionDispatcher] so the native lib load +
     * schema work never runs on the caller's (potentially main) thread.
     */
    suspend fun createAsync(): CuppedDatabase =
        withContext(transactionDispatcher.dispatcher) { create() }
}
