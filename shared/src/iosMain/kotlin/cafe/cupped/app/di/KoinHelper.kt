package cafe.cupped.app.di

import cafe.cupped.app.logging.NapierInit
import cafe.cupped.app.navigation.PathConfigRouter
import cafe.cupped.app.network.CuppedApiClient
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatformTools

object KoinHelper {
    /**
     * Initialize Koin + Napier. Idempotent — safe to call multiple times.
     * @param baseUrl The Phoenix server base URL. No default — callers must
     *   supply the correct URL for the target environment.
     */
    fun initKoin(baseUrl: String) {
        if (KoinPlatformTools.defaultContext().getOrNull() != null) return
        NapierInit.init()
        startKoin {
            modules(sharedModule(baseUrl), platformModule())
        }
    }

    // Typed factory methods — Swift cannot call generic koin.get<T>()
    fun getCuppedApiClient(): CuppedApiClient =
        KoinPlatformTools.defaultContext().get().get()

    fun getPathConfigRouter(): PathConfigRouter =
        KoinPlatformTools.defaultContext().get().get()
}
