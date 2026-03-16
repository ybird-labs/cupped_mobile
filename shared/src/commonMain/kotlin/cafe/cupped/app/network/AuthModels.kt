package cafe.cupped.app.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// -- Magic Link Request --

@Serializable
data class MagicLinkRequest(
    val email: String
)

@Serializable
data class MagicLinkResponse(
    val message: String
)

// -- Token Verification --

@Serializable
data class VerifyTokenRequest(
    val token: String
)

@Serializable
data class VerifyTokenResponse(
    @SerialName("token")
    val bearerToken: String,
    val user: AuthenticatedUserResponse? = null
)

// -- Auth Error --

@Serializable
data class AuthErrorResponse(
    val errors: AuthErrorDetail? = null
)

@Serializable
data class AuthenticatedUserResponse(
    val id: String,
    val email: String,
    val role: String,
    @SerialName("confirmed_at")
    val confirmedAt: String? = null
)

@Serializable
data class AuthErrorDetail(
    val detail: String
)
