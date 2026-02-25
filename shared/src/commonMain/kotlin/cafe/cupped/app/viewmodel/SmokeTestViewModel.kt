package cafe.cupped.app.viewmodel

import cafe.cupped.app.network.CuppedApiClient
import com.rickclephas.kmp.observableviewmodel.MutableStateFlow
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import com.rickclephas.kmp.observableviewmodel.stateIn
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

open class SmokeTestViewModel(
    private val apiClient: CuppedApiClient
) : ViewModel() {

    private val _greeting = MutableStateFlow(viewModelScope, "Hello from KMP!")
    val greeting: StateFlow<String> = _greeting

    private val _isHealthy = MutableStateFlow(viewModelScope, false)
    val isHealthy: StateFlow<Boolean> = _isHealthy

    val status: StateFlow<String> = _isHealthy.map { healthy ->
        if (healthy) "Server: Connected" else "Server: Not checked"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Server: Not checked")

    fun checkHealth() {
        Napier.d("SmokeTestViewModel: checking health...")
        viewModelScope.coroutineScope.launch {
            val healthy = apiClient.healthCheck()
            _isHealthy.value = healthy
            _greeting.value = if (healthy) {
                "KMP -> Server -> SwiftUI"
            } else {
                "KMP -> SwiftUI (server unreachable)"
            }
            Napier.d("SmokeTestViewModel: health=$healthy")
        }
    }
}
