package cafe.cupped.app.viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals

class AuthViewModelTest {

    @Test
    fun userFriendlyErrorPreservesSemanticAuthFailures() {
        val message = AuthViewModel.userFriendlyError(
            error = Exception("Invalid or expired token"),
            fallback = "Failed to verify token"
        )

        assertEquals(
            "This link has expired or is invalid. Please request a new one.",
            message
        )
    }

    @Test
    fun userFriendlyErrorPreservesExplicitVerifyContractFailures() {
        val message = AuthViewModel.userFriendlyError(
            error = Exception("Unexpected verify response from server"),
            fallback = "Failed to verify token"
        )

        assertEquals("Unexpected verify response from server", message)
    }

    @Test
    fun userFriendlyErrorSanitizesShortSensitiveRequestFailures() {
        val message = AuthViewModel.userFriendlyError(
            error = Exception("User not found"),
            fallback = "Failed to send magic link"
        )

        assertEquals(
            "Unable to continue with magic link sign-in. Please try again.",
            message
        )
    }
}
