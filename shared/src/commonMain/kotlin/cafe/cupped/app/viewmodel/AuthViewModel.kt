package cafe.cupped.app.viewmodel

import cafe.cupped.app.network.CuppedApiClient
import com.rickclephas.kmp.observableviewmodel.MutableStateFlow
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the authentication screen.
 *
 * Drives the SwiftUI login/register views via KMP-ObservableViewModel.
 * Does NOT manage session cookies — that's handled on the iOS side
 * by MobileSessionClient after receiving a bearer token.
 */
sealed interface AuthUiState {
    /** Initial state — no action taken yet. */
    data object Idle : AuthUiState

    /** API call in progress (magic link request or token verification). */
    data object Loading : AuthUiState

    /** Magic link email sent successfully. */
    data class MagicLinkSent(val email: String) : AuthUiState

    /**
     * Token verified — bearer token available.
     * The iOS side takes this token and exchanges it for a session
     * cookie via MobileSessionClient.
     */
    data class Authenticated(val bearerToken: String) : AuthUiState

    /** An error occurred. */
    data class Error(val message: String) : AuthUiState
}

/**
 * ViewModel for the native login/register screens.
 *
 * ## Flow
 * 1. User enters email → [requestMagicLink] → state becomes
 *    [AuthUiState.MagicLinkSent]
 * 2. User clicks magic link in email → app receives token via
 *    deep link → [verifyToken] → state becomes
 *    [AuthUiState.Authenticated]
 * 3. iOS side takes bearerToken from Authenticated state and
 *    passes to MobileSessionClient for cookie exchange
 *
 * ## State Management
 * Uses KMP-ObservableViewModel StateFlow, same pattern as
 * SmokeTestViewModel. SwiftUI observes via @StateObject.
 */
open class AuthViewModel(
    private val apiClient: CuppedApiClient
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(
        viewModelScope, AuthUiState.Idle
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /**
     * Requests a magic link for the given email address.
     *
     * Transitions: Idle/Error → Loading → MagicLinkSent/Error
     */
    fun requestMagicLink(email: String) {
        viewModelScope.coroutineScope.launch {
            _uiState.value = AuthUiState.Loading
            apiClient.requestMagicLink(email).fold(
                onSuccess = {
                    _uiState.value = AuthUiState.MagicLinkSent(email)
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Error(
                        error.message ?: "Failed to send magic link"
                    )
                }
            )
        }
    }

    /**
     * Verifies a magic link token received via deep link.
     *
     * Transitions: any → Loading → Authenticated/Error
     */
    fun verifyToken(token: String) {
        viewModelScope.coroutineScope.launch {
            _uiState.value = AuthUiState.Loading
            apiClient.verifyMagicLinkToken(token).fold(
                onSuccess = { response ->
                    _uiState.value = AuthUiState.Authenticated(
                        response.bearerToken
                    )
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Error(
                        error.message ?: "Failed to verify token"
                    )
                }
            )
        }
    }

    /**
     * Resets state to Idle. Used when user wants to try a
     * different email or dismiss an error.
     */
    fun reset() {
        _uiState.value = AuthUiState.Idle
    }
}
