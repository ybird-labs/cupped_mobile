package cafe.cupped.app.di

import cafe.cupped.app.db.CuppedDatabaseFactory
import cafe.cupped.app.db.TransactionDispatcher
import cafe.cupped.app.db.ioDispatcher
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Provides the local SQLDelight database. The [cafe.cupped.app.db.DatabaseDriverFactory]
 * and [cafe.cupped.app.db.DatabaseKeyProvider] are supplied by each platform's
 * `platformModule()` (Android/iOS) since they need a Context / Keychain.
 *
 * Registered on BOTH platforms: iOS via [KoinHelper.initKoin], Android via the
 * `CuppedApplication` `startKoin { ... databaseModule() }` (architecture §S5/#5).
 *
 * Off-main-thread open contract (architecture §S4): the production module
 * exposes ONLY [CuppedDatabaseFactory]; it does NOT register an eager
 * `single<CuppedDatabase>`, so the database can never be opened synchronously on
 * the main thread via a stray Koin `get()`. Consumers (repository / sync engine)
 * obtain the database via [CuppedDatabaseFactory.createAsync] from a coroutine,
 * which opens on the injected [TransactionDispatcher] (IO in prod). Tests that
 * need a ready database construct their own driver (see SchemaMigrationTest) or
 * register a test-only single.
 */
fun databaseModule(): Module = module {
    single { TransactionDispatcher(ioDispatcher()) }
    single { CuppedDatabaseFactory(driverFactory = get(), transactionDispatcher = get()) }
}
