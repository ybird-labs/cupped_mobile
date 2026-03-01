package cafe.cupped.app.di

import cafe.cupped.app.isDebug
import cafe.cupped.app.logging.NapierInit
import cafe.cupped.app.navigation.PathConfigRouter
import cafe.cupped.app.network.CuppedApiClient
import cafe.cupped.app.viewmodel.AuthViewModel
import cafe.cupped.app.viewmodel.SmokeTestViewModel
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.mp.KoinPlatformTools
import platform.Foundation.NSLock

object KoinHelper {
    private val lock = NSLock()

    /**
     * Initialize Koin + Napier. Idempotent and thread-safe — safe to call
     * multiple times from any thread; only the first call takes effect.
     * @param baseUrl The Phoenix server base URL. No default — callers must
     *   supply the correct URL for the target environment.
     */
    fun initKoin(baseUrl: String) {
        lock.lock()
        try {
            if (KoinPlatformTools.defaultContext().getOrNull() != null) return
            if (isDebug) {
                NapierInit.init()
            }
            startKoin {
                modules(sharedModule(baseUrl), platformModule())
            }
        } finally {
            lock.unlock()
        }
    }

    // Typed factory methods — Swift cannot call generic koin.get<T>()
    fun getCuppedApiClient(): CuppedApiClient =
        KoinPlatformTools.defaultContext().get().get()

    fun getPathConfigRouter(): PathConfigRouter =
        KoinPlatformTools.defaultContext().get().get()

    fun makeSmokeTestViewModel(): SmokeTestViewModel =
        KoinPlatformTools.defaultContext().get().get()

    fun makeAuthViewModel(): AuthViewModel =
        KoinPlatformTools.defaultContext().get().get()

    /**
     * Retrieves the Phoenix server base URL from the Koin
     * dependency graph.
     *
     * This is the same value passed to [initKoin] from
     * Config.xcconfig via Info.plist. Swift code calls this
     * to construct WebView URLs (e.g., `baseUrl + "/feed"`)
     * and the mobile-session exchange endpoint.
     *
     * @return The base URL string (e.g.,
     *   `"http://localhost:4000"`), without a trailing slash.
     * @throws IllegalStateException if Koin has not been
     *   initialized via [initKoin].
     */
    fun getBaseUrl(): String =
        KoinPlatformTools.defaultContext()
            .get().get(named("baseUrl"))
}
