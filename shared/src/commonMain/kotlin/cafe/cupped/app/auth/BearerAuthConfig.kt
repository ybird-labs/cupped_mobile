package cafe.cupped.app.auth

import cafe.cupped.app.api.generated.models.VerifyResponse as GeneratedVerifyResponse
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Installs the Ktor [Auth] plugin with a `bearer { }` provider that is shared
 * by both platform engines (OkHttp on Android, Darwin on iOS) so sync calls
 * authenticate uniformly by Bearer on both platforms (architecture §11).
 *
 * Design notes (architecture §11, research §"Ktor Bearer auth"):
 *  - `loadTokens` reads from the persistent [TokenStore].
 *  - `refreshTokens` fires on a 401, calls `POST /api/v1/auth/refresh` with the
 *    current token, persists the new token, and returns it for retry.
 *  - The refresh path is guarded by a [Mutex] to work around Ktor's known
 *    refresh de-duplication issues (KTOR-5681 / 3795 / 5879): concurrent 401s
 *    must not each fire their own refresh and stomp each other. Inside the lock
 *    we re-read the store first, so a request that lost the race picks up the
 *    token a concurrent refresh already wrote instead of refreshing again.
 *
 * The Bearer provider is installed *alongside* the existing magic-link/session
 * flow — it does not replace it. Endpoints that don't need a token simply have
 * no stored token (loadTokens returns null) and send no Authorization header.
 */
fun HttpClientConfig<*>.installBearerAuth(
    tokenStore: TokenStore,
    baseUrl: String,
) {
    val refreshUrl = "${baseUrl.trimEnd('/')}/api/v1/auth/refresh"
    // Serializes all refresh attempts. One client == one config == one Mutex.
    val refreshMutex = Mutex()

    install(Auth) {
        bearer {
            loadTokens {
                tokenStore.getTokens()?.toBearerTokens()
            }

            refreshTokens {
                refreshMutex.withLock {
                    // Re-read: a concurrent 401 may have already refreshed while
                    // we waited for the lock. If the token changed since the one
                    // that triggered this 401, reuse it instead of refreshing again.
                    val current = tokenStore.getTokens()
                    val triggering = oldTokens?.accessToken
                    if (current != null && current.accessToken != triggering) {
                        return@withLock current.toBearerTokens()
                    }

                    val token = current?.accessToken
                    if (token == null) {
                        Napier.w("Bearer refresh requested but no token stored; cannot refresh")
                        return@withLock null
                    }

                    try {
                        // `client` here is the auth-plugin-provided client; the
                        // refresh request is sent WITHOUT the bearer provider's
                        // automatic retry. We attach the current token explicitly.
                        val response: HttpResponse = client.post(refreshUrl) {
                            markAsRefreshTokenRequest()
                            headers {
                                append(HttpHeaders.Authorization, "Bearer $token")
                            }
                        }
                        if (!response.status.isSuccess()) {
                            Napier.w("Bearer refresh failed: HTTP ${response.status.value}")
                            return@withLock null
                        }
                        val newToken = response.body<GeneratedVerifyResponse>().token
                        if (newToken.isNullOrBlank()) {
                            Napier.w("Bearer refresh succeeded but response carried no token")
                            return@withLock null
                        }
                        val newTokens = StoredTokens.single(newToken)
                        tokenStore.saveTokens(newTokens)
                        newTokens.toBearerTokens()
                    } catch (e: Exception) {
                        Napier.e("Bearer refresh threw", e)
                        null
                    }
                }
            }

            // Always attach the bearer to requests once we have a token, without
            // waiting for a 401 first. Refresh still handles expiry.
            sendWithoutRequest { true }
        }
    }
}

private fun StoredTokens.toBearerTokens(): BearerTokens =
    BearerTokens(accessToken = accessToken, refreshToken = refreshToken)
