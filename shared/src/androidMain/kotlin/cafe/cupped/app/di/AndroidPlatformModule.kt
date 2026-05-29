package cafe.cupped.app.di

import cafe.cupped.app.auth.AndroidTokenStore
import cafe.cupped.app.auth.TokenStore
import cafe.cupped.app.auth.installBearerAuth
import cafe.cupped.app.db.AndroidDatabaseDriverFactory
import cafe.cupped.app.db.AndroidDatabaseKeyProvider
import cafe.cupped.app.db.DatabaseDriverFactory
import cafe.cupped.app.db.DatabaseKeyProvider
import cafe.cupped.app.network.configureHttpLogging
import cafe.cupped.app.platform.KeystoreSecretStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

actual fun platformModule() = module {
    // Single shared Keystore-backed secret store (one Keystore key, one prefs
    // file) used by BOTH the token store and the DB key provider — avoids the
    // cold-start key-creation race of two instances sharing one alias.
    single { KeystoreSecretStore(androidContext()) }
    single<TokenStore> { AndroidTokenStore(get()) }
    single<DatabaseKeyProvider> { AndroidDatabaseKeyProvider(get()) }
    single<DatabaseDriverFactory> {
        AndroidDatabaseDriverFactory(context = androidContext(), keyProvider = get())
    }
    single<HttpClient> {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                })
            }
            installBearerAuth(
                tokenStore = get(),
                baseUrl = get(named("baseUrl")),
            )
            configureHttpLogging()
        }
    }
}
