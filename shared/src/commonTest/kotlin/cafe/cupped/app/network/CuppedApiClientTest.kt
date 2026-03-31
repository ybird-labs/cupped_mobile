package cafe.cupped.app.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CuppedApiClientTest {

    @Test
    fun verifyMagicLinkTokenDecodesBackendTokenField() = runTest {
        val client = makeApiClient(
            status = HttpStatusCode.Created,
            body = """
                {
                  "token": "live_api_token_123",
                  "user": {
                    "id": "user-1",
                    "email": "coffee@cupped.cafe",
                    "role": "user",
                    "confirmed_at": "2026-01-15T10:30:00Z"
                  }
                }
            """.trimIndent()
        )

        val result = client.verifyMagicLinkToken("SFMyNTY.valid-token")

        assertTrue(result.isSuccess)
        assertEquals("live_api_token_123", result.getOrThrow().bearerToken)
        assertEquals("coffee@cupped.cafe", result.getOrThrow().user?.email)
    }

    @Test
    fun verifyMagicLinkTokenSurfacesBackendInvalidTokenDetail() = runTest {
        val client = makeApiClient(
            status = HttpStatusCode.Unauthorized,
            body = """{"errors":{"detail":"Invalid or expired token"}}"""
        )

        val result = client.verifyMagicLinkToken("SFMyNTY.invalid-token")

        assertTrue(result.isFailure)
        assertEquals(
            "Invalid or expired token",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun verifyMagicLinkTokenSurfacesBackendRateLimitDetail() = runTest {
        val client = makeApiClient(
            status = HttpStatusCode.TooManyRequests,
            body = """{"errors":{"detail":"Too many requests"}}"""
        )

        val result = client.verifyMagicLinkToken("SFMyNTY.rate-limited")

        assertTrue(result.isFailure)
        assertEquals(
            "Too many requests",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun verifyMagicLinkTokenReturnsExplicitMessageForUnexpectedSuccessPayload() = runTest {
        val client = makeApiClient(
            status = HttpStatusCode.Created,
            body = """{"bearer_token":"legacy-token"}"""
        )

        val result = client.verifyMagicLinkToken("SFMyNTY.unexpected-payload")

        assertTrue(result.isFailure)
        assertEquals(
            "Unexpected verify response from server",
            result.exceptionOrNull()?.message
        )
    }

    private fun makeApiClient(
        status: HttpStatusCode,
        body: String
    ): CuppedApiClient {
        val engine = MockEngine { request ->
            assertEquals("/api/v1/auth/verify", request.url.encodedPath)
            assertEquals("POST", request.method.value)

            respond(
                content = body,
                status = status,
                headers = headersOf(
                    HttpHeaders.ContentType,
                    ContentType.Application.Json.toString()
                )
            )
        }

        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        encodeDefaults = true
                    }
                )
            }
        }

        return CuppedApiClient(
            httpClient = httpClient,
            baseUrl = "https://cupped.fly.dev"
        )
    }
}
