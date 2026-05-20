package cafe.cupped.app.network

import cafe.cupped.app.api.generated.models.ErrorResponse as GeneratedErrorResponse
import cafe.cupped.app.api.generated.models.MagicLinkResponse as GeneratedMagicLinkResponse
import cafe.cupped.app.api.generated.models.UserResponse as GeneratedUserResponse
import cafe.cupped.app.api.generated.models.VerifyResponse as GeneratedVerifyResponse

internal fun GeneratedMagicLinkResponse.toAuthModel(): MagicLinkResponse =
    MagicLinkResponse(
        message = requireApiField(message, "Magic link response message")
    )

internal fun GeneratedVerifyResponse.toAuthModel(): VerifyTokenResponse {
    val bearerToken = token?.ifBlank { null }
        ?: throw IllegalArgumentException("Verify response is missing bearer token")

    return VerifyTokenResponse(
        bearerToken = bearerToken,
        user = user?.toAuthModel()
    )
}

internal fun GeneratedUserResponse.toAuthModel(): AuthenticatedUserResponse =
    AuthenticatedUserResponse(
        id = requireApiField(id, "User id"),
        email = requireApiField(email, "User email"),
        role = requireApiField(role?.value, "User role"),
        confirmedAt = confirmedAt
    )

internal fun GeneratedErrorResponse.detailOrNull(): String? =
    errors?.detail?.ifBlank { null }

private fun requireApiField(value: String?, fieldName: String): String =
    value?.ifBlank { null }
        ?: throw IllegalArgumentException("$fieldName is missing from API response")
