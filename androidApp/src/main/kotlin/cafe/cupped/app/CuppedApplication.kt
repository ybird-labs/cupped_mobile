package cafe.cupped.app

import android.app.Application
import cafe.cupped.app.di.databaseModule
import cafe.cupped.app.di.platformModule
import cafe.cupped.app.di.sharedModule
import cafe.cupped.app.logging.NapierInit
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * Android application entry point that starts Koin.
 *
 * Previously Koin was only ever started on iOS (via `KoinHelper.initKoin`),
 * so the `androidContext()`-dependent providers (TokenStore, DatabaseKeyProvider,
 * DatabaseDriverFactory, HttpClient) and `databaseModule()` were never
 * registered on Android (architecture review #4/#5). This bootstrap mirrors the
 * EXACT module list iOS uses — `sharedModule(baseUrl)`, `platformModule()`,
 * `databaseModule()` — plus `androidContext()`.
 *
 * [BASE_URL][BuildConfig.BASE_URL] comes from the build type (debug ->
 * 10.0.2.2:4000, release -> production host). iOS sources the same value from
 * Config.xcconfig via Info.plist.
 */
class CuppedApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (isDebug) {
            NapierInit.init()
        }
        startKoin {
            androidContext(this@CuppedApplication)
            modules(
                sharedModule(BuildConfig.BASE_URL),
                platformModule(),
                databaseModule(),
            )
        }
    }
}
