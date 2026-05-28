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
 *
 * Registered on BOTH platforms: iOS via [KoinHelper.initKoin], Android via the
 * `CuppedApplication` `startKoin { ... databaseModule() }` (architecture §S5/#5).
 *
 * Off-main-thread open contract (architecture §S4): the [CuppedDatabase] single
 * is resolved lazily, and [CuppedDatabaseFactory.createAsync] opens on the
 * injected [TransactionDispatcher] (IO in prod). Consumers (repository / sync
 * engine) MUST obtain the database via [CuppedDatabaseFactory.createAsync] from
 * a coroutine, NOT by touching the eager `CuppedDatabase` single on the main
 * thread. The eager single is kept only for tests / non-UI callers that already
 * control their thread.
 */
fun databaseModule(): Module = module {
    single { TransactionDispatcher(ioDispatcher()) }
    single { CuppedDatabaseFactory(driverFactory = get(), transactionDispatcher = get()) }
    // Lazy: Koin only constructs this on first get(). Prefer createAsync() off
    // the main thread; this single exists for callers that already control
    // their thread (e.g. JVM tests).
    single<CuppedDatabase> { get<CuppedDatabaseFactory>().create() }
}
