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
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodedPath
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
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
    val trimmedBase = baseUrl.trimEnd('/')
    val refreshUrl = "$trimmedBase/api/v1/auth/refresh"
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
                        // HARD refresh failure: the session is expired/revoked
                        // (refresh endpoint itself returns 401/Unauthorized).
                        // Clear the store and return null so the app drops to
                        // signed-out instead of re-attaching a dead token forever.
                        if (response.status == HttpStatusCode.Unauthorized) {
                            Napier.w("Bearer refresh rejected (401); clearing tokens -> signed out")
                            tokenStore.clear()
                            return@withLock null
                        }
                        if (!response.status.isSuccess()) {
                            // Transient (5xx/network-ish) — keep tokens, retry later.
                            Napier.w("Bearer refresh failed: HTTP ${response.status.value}")
                            return@withLock null
                        }
                        val newToken = response.body<GeneratedVerifyResponse>().token
                        if (newToken.isNullOrBlank()) {
                            Napier.w("Bearer refresh succeeded but response carried no token")
                            return@withLock null
                        }
                        // No-rotation guard: a 200 that returns the SAME token as
                        // the one that just 401'd is not a real refresh. Treat as
                        // an auth failure: clear + signed-out rather than looping
                        // forever re-attaching an identical, already-rejected token.
                        if (newToken == token) {
                            Napier.w("Bearer refresh returned an unrotated token; clearing -> signed out")
                            tokenStore.clear()
                            return@withLock null
                        }
                        val newTokens = StoredTokens.single(newToken)
                        tokenStore.saveTokens(newTokens)
                        newTokens.toBearerTokens()
                    } catch (e: CancellationException) {
                        // Never swallow cancellation — let it propagate so the
                        // calling coroutine's structured concurrency is honored.
                        throw e
                    } catch (e: Exception) {
                        Napier.e("Bearer refresh threw", e)
                        null
                    }
                }
            }

            // Realm scoping: attach the bearer to all requests EXCEPT the
            // unauthenticated magic-link/refresh auth endpoints. Without this a
            // stale token rides along on /auth/verify, requestMagicLink, and
            // /auth/refresh (the refresh request attaches its token explicitly
            // above). Scoped by path so those endpoints stay token-free even
            // once a token is populated (Phase 4).
            sendWithoutRequest { request ->
                val path = request.url.encodedPath
                !isUnauthenticatedAuthPath(path)
            }
        }
    }
}

/**
 * True for the magic-link / token-exchange endpoints that must NOT carry a
 * bearer token (they establish or rotate the session themselves).
 */
private fun isUnauthenticatedAuthPath(encodedPath: String): Boolean =
    // Endpoints from the Brewer OpenAPI spec that establish/rotate the session
    // and must not carry a (possibly stale) bearer:
    //   POST /api/v1/auth/magic-link  (requestMagicLink)
    //   POST /api/v1/auth/verify      (magic-link -> token exchange)
    //   POST /api/v1/auth/refresh     (token rotation; attaches its own token)
    encodedPath.endsWith("/auth/magic-link") ||
        encodedPath.endsWith("/auth/verify") ||
        encodedPath.endsWith("/auth/refresh")

private fun StoredTokens.toBearerTokens(): BearerTokens =
    BearerTokens(accessToken = accessToken, refreshToken = refreshToken)
