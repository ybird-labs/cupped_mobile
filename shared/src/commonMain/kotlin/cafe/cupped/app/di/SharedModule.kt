package cafe.cupped.app.di

import cafe.cupped.app.navigation.PathConfigRouter
import cafe.cupped.app.network.CuppedApiClient
import cafe.cupped.app.viewmodel.SmokeTestViewModel
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun sharedModule(baseUrl: String): Module = module {
    single(named("baseUrl")) { baseUrl }
    single { PathConfigRouter() }
    single { CuppedApiClient(httpClient = get(), baseUrl = get(named("baseUrl"))) }
    factory { SmokeTestViewModel(get()) }
}
