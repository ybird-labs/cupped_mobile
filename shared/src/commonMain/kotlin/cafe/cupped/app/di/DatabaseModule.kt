package cafe.cupped.app.di

import cafe.cupped.app.db.CuppedDatabase
import cafe.cupped.app.db.CuppedDatabaseFactory
import cafe.cupped.app.db.TransactionDispatcher
import cafe.cupped.app.db.ioDispatcher
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Provides the local SQLDelight database. The [cafe.cupped.app.db.DatabaseDriverFactory]
 * and [cafe.cupped.app.db.DatabaseKeyProvider] are supplied by each platform's
 * `platformModule()` (Android/iOS) since they need a Context / Keychain.
 */
fun databaseModule(): Module = module {
    single { TransactionDispatcher(ioDispatcher()) }
    single { CuppedDatabaseFactory(driverFactory = get()) }
    single<CuppedDatabase> { get<CuppedDatabaseFactory>().create() }
}
