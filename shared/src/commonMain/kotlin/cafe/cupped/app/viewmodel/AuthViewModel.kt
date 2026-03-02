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
        if (_uiState.value is AuthUiState.Loading) return
        viewModelScope.coroutineScope.launch {
            _uiState.value = AuthUiState.Loading
            apiClient.requestMagicLink(email).fold(
                onSuccess = {
                    _uiState.value = AuthUiState.MagicLinkSent(email)
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Error(
                        userFriendlyError(error, "Failed to send magic link")
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
        if (_uiState.value is AuthUiState.Loading) return
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
                        userFriendlyError(error, "Failed to verify token")
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

    companion object {
        /**
         * Converts a raw platform exception message into a
         * user-friendly string.
         *
         * On iOS, Ktor wraps NSURLError into the exception
         * message — this includes the full NSError description
         * with domain, code, and UserInfo dictionary. On Android,
         * messages are typically already clean.
         *
         * This runs in commonMain so both platforms get the
         * same sanitization.
         */
        internal fun userFriendlyError(
            error: Throwable,
            fallback: String
        ): String {
            val msg = error.message ?: return fallback

            // iOS NSURLError dumps contain these markers
            val isNSURLError = msg.contains("NSURLErrorDomain")
                || msg.contains("kCFStreamError")
                || msg.contains("CFNetwork")

            if (isNSURLError) {
                return when {
                    msg.contains("Could not connect to the server", ignoreCase = true) ->
                        "Unable to reach the server. Check your connection and try again."
                    msg.contains("timed out", ignoreCase = true) ->
                        "The request timed out. Please try again."
                    msg.contains("not connected to the internet", ignoreCase = true) ->
                        "No internet connection. Please check your network."
                    msg.contains("cannot find host", ignoreCase = true) ->
                        "Server not found. Please try again later."
                    else -> "Unable to connect. Please try again."
                }
            }

            // Clean, short messages pass through (e.g. "HTTP 401")
            if (msg.length < 120 && !msg.contains("{")) return msg

            // Anything else — use fallback
            return fallback
        }
    }
}
