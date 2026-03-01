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
    @SerialName("bearer_token")
    val bearerToken: String
)

// -- Auth Error --

@Serializable
data class AuthErrorResponse(
    val error: String
)
