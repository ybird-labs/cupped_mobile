package cafe.cupped.app.di

import cafe.cupped.app.auth.AndroidTokenStore
import cafe.cupped.app.auth.TokenStore
import cafe.cupped.app.auth.installBearerAuth
import cafe.cupped.app.network.configureHttpLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

actual fun platformModule() = module {
    single<TokenStore> { AndroidTokenStore(androidContext()) }
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
