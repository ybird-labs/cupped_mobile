package cafe.cupped.app.api

import cafe.cupped.app.api.generated.models.BrewLog
import cafe.cupped.app.api.generated.models.ErrorResponse
import cafe.cupped.app.api.generated.models.MagicLinkRequest
import cafe.cupped.app.api.generated.models.UserResponse
import cafe.cupped.app.api.generated.models.VerifyRequest
import cafe.cupped.app.api.generated.models.VerifyResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class GeneratedDtoSerializationTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun verifyResponseDecodesTokenAndNestedUserWithConfirmedAt() {
        val response = json.decodeFromString<VerifyResponse>(
            """
            {
              "token": "live_api_token_123",
              "top_level_ignored": true,
              "user": {
                "id": "550e8400-e29b-41d4-a716-446655440000",
                "email": "coffee@cupped.cafe",
                "role": "user",
                "confirmed_at": "2026-01-15T10:30:00Z",
                "ignored_field": "ignored"
              }
            }
            """.trimIndent()
        )

        assertEquals("live_api_token_123", response.token)
        assertEquals("550e8400-e29b-41d4-a716-446655440000", response.user?.id)
        assertEquals("coffee@cupped.cafe", response.user?.email)
        assertEquals(UserResponse.Role.user, response.user?.role)
        assertEquals("2026-01-15T10:30:00Z", response.user?.confirmedAt)
    }

    @Test
    fun magicLinkRequestSerializesEmail() {
        val request = MagicLinkRequest(email = "coffee@cupped.cafe")

        assertEquals(
            """{"email":"coffee@cupped.cafe"}""",
            json.encodeToString(request)
        )
    }

    @Test
    fun verifyRequestSerializesToken() {
        val request = VerifyRequest(token = "SFMyNTY.valid-token")

        assertEquals(
            """{"token":"SFMyNTY.valid-token"}""",
            json.encodeToString(request)
        )
    }

    @Test
    fun errorResponseDecodesErrorsDetail() {
        val response = json.decodeFromString<ErrorResponse>(
            """
            {
              "errors": {
                "detail": "Invalid or expired token"
              }
            }
            """.trimIndent()
        )

        assertEquals("Invalid or expired token", response.errors?.detail)
    }

    @Test
    fun brewLogDecodesRequiredNullableLoggedAtAndSnakeCaseFields() {
        val brewLog = json.decodeFromString<BrewLog>(
            """
            {
              "id": "660e8400-e29b-41d4-a716-446655440000",
              "logged_at": null,
              "location_name": "Cupped Test Bar",
              "post_id": "770e8400-e29b-41d4-a716-446655440000",
              "rating": 5,
              "bean": {
                "id": "880e8400-e29b-41d4-a716-446655440000",
                "name": "Ethiopia Test Lot",
                "slug": "ethiopia-test-lot",
                "country": "Ethiopia",
                "region": "Yirgacheffe",
                "process": "washed",
                "roast_level": 3
              }
            }
            """.trimIndent()
        )

        assertEquals("660e8400-e29b-41d4-a716-446655440000", brewLog.id)
        assertNull(brewLog.loggedAt)
        assertEquals("Cupped Test Bar", brewLog.locationName)
        assertEquals("770e8400-e29b-41d4-a716-446655440000", brewLog.postId)
        assertEquals(5, brewLog.rating)
        assertEquals("Ethiopia Test Lot", brewLog.bean.name)
        assertEquals("ethiopia-test-lot", brewLog.bean.slug)
        assertEquals(3, brewLog.bean.roastLevel)
    }

    @Test
    fun userResponseRoleEnumSerialNamesDecodeUserAndAdmin() {
        val user = json.decodeFromString<UserResponse>(
            """
            {
              "id": "990e8400-e29b-41d4-a716-446655440000",
              "email": "user@cupped.cafe",
              "role": "user",
              "confirmed_at": null
            }
            """.trimIndent()
        )
        val admin = json.decodeFromString<UserResponse>(
            """
            {
              "id": "aa0e8400-e29b-41d4-a716-446655440000",
              "email": "admin@cupped.cafe",
              "role": "admin",
              "confirmed_at": null
            }
            """.trimIndent()
        )

        assertEquals(UserResponse.Role.user, user.role)
        assertEquals(UserResponse.Role.admin, admin.role)
    }
}
