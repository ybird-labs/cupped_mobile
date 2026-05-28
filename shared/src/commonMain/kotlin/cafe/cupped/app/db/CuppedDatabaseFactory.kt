package cafe.cupped.app.db

import app.cash.sqldelight.db.SqlDriver

/**
 * Builds the [CuppedDatabase] from a platform [DatabaseDriverFactory].
 *
 * The transaction [dispatcher] is injectable (not hard-coded) so future
 * invariant tests can drive transactions under `kotlinx-coroutines-test`
 * virtual time (architecture §10/§18, research: any un-injected
 * `withContext(Dispatchers.X)` escapes virtual time). SQLDelight 2.1.0's
 * `SuspendingTransacter.TransactionDispatcher` is configured by the caller of
 * suspending transactions; this wrapper simply carries the chosen dispatcher
 * alongside the database so repositories/sync engine read it from one place.
 */
class CuppedDatabaseFactory(
    private val driverFactory: DatabaseDriverFactory,
) {
    fun create(): CuppedDatabase {
        val driver: SqlDriver = driverFactory.createDriver()
        return CuppedDatabase(driver)
    }
}
