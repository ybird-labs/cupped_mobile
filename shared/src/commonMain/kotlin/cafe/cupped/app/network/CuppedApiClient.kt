package cafe.cupped.app.network

import cafe.cupped.app.isDebug
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.json.Json

internal class CuppedApiClient(
    private val httpClient: HttpClient,
    baseUrl: String
) {
    private val baseUrl: String = baseUrl.trimEnd('/')
    private val authBaseUrl: String = "$baseUrl/api/v1/auth"
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Returns true only if the server responds with a 2xx status code.
     * Does NOT return true on connection errors, timeouts, or non-2xx responses.
     */
    suspend fun healthCheck(): Boolean {
        return try {
            val response: HttpResponse = httpClient.get("${this.baseUrl}/api/health")
            val healthy = response.status.value in 200..299
            Napier.d("Health check: ${response.status.value} -> healthy=$healthy")
            healthy
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.e("Health check failed", e)
            false
        }
    }

    /**
     * Requests a magic link email for the given address.
     * POST /api/v1/auth/magic-link
     *
     * Always returns success to the caller (the server never reveals
     * whether the email exists — anti-enumeration). The actual success/
     * failure is whether the HTTP call completed without error.
     *
     * @return Result.success with the server message, or Result.failure
     *   with the exception.
     */
    suspend fun requestMagicLink(email: String): Result<MagicLinkResponse> {
        return try {
            val response = httpClient.post("$authBaseUrl/magic-link") {
                contentType(ContentType.Application.Json)
                setBody(MagicLinkRequest(email = email))
            }
            if (response.status.value in 200..299) {
                Result.success(response.body<MagicLinkResponse>())
            } else {
                val bodyText = response.bodyAsText()
                Result.failure(
                    Exception(
                        parseErrorMessage(
                            bodyText = bodyText,
                            statusCode = response.status.value,
                            fallbackPrefix = "Request failed"
                        )
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.e("Magic link request failed", e)
            Result.failure(e)
        }
    }

    /**
     * Verifies a magic link token and returns a bearer token.
     * POST /api/v1/auth/verify
     *
     * The returned bearer token is used by the iOS side to exchange
     * for a session cookie via MobileSessionClient.
     *
     * @return Result.success with the bearer token response, or
     *   Result.failure with the exception.
     */
    suspend fun verifyMagicLinkToken(token: String): Result<VerifyTokenResponse> {
        return try {
            val response = httpClient.post("$authBaseUrl/verify") {
                contentType(ContentType.Application.Json)
                setBody(VerifyTokenRequest(token = token))
            }
            if (response.status.value in 200..299) {
                val bodyText = response.bodyAsText()
                try {
                    Result.success(
                        json.decodeFromString<VerifyTokenResponse>(bodyText)
                    )
                } catch (e: Exception) {
                    debugVerifyResponse(
                        token = token,
                        statusCode = response.status.value,
                        contentType = response.headers[HttpHeaders.ContentType],
                        bodyText = bodyText,
                        reason = "decode failed: ${e.message}"
                    )
                    Result.failure(Exception("Unexpected verify response from server"))
                }
            } else {
                val bodyText = response.bodyAsText()
                debugVerifyResponse(
                    token = token,
                    statusCode = response.status.value,
                    contentType = response.headers[HttpHeaders.ContentType],
                    bodyText = bodyText,
                    reason = "non-success response"
                )
                Result.failure(
                    Exception(
                        parseErrorMessage(
                            bodyText = bodyText,
                            statusCode = response.status.value,
                            fallbackPrefix = "Verification failed"
                        )
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.e("Token verification failed", e)
            Result.failure(e)
        }
    }

    private fun parseErrorMessage(
        bodyText: String,
        statusCode: Int,
        fallbackPrefix: String
    ): String {
        val fallbackMessage = "$fallbackPrefix: HTTP $statusCode"
        if (bodyText.isBlank()) return fallbackMessage

        return try {
            val error = json.decodeFromString<AuthErrorResponse>(bodyText)
            error.errors?.detail?.ifBlank { null } ?: fallbackMessage
        } catch (_: Exception) {
            fallbackMessage
        }
    }

    private fun debugVerifyResponse(
        token: String,
        statusCode: Int,
        contentType: String?,
        bodyText: String,
        reason: String
    ) {
        if (!isDebug) return

        Napier.d(
            tag = "AUTH",
            message = buildString {
                append("verifyMagicLinkToken ")
                append(reason)
                append(" status=")
                append(statusCode)
                append(" contentType=")
                append(contentType ?: "unknown")
                append(" tokenLength=")
                append(token.length)
                append(" bodySnippet=")
                append(bodyText.take(256))
            }
        )
    }
}
