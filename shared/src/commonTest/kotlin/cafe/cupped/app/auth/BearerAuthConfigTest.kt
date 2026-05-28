package cafe.cupped.app.auth

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodedPath
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** In-memory [TokenStore] for tests. */
private class FakeTokenStore(initial: StoredTokens? = null) : TokenStore {
    private var tokens: StoredTokens? = initial
    var saveCount = 0
        private set
    var clearCount = 0
        private set

    override fun getTokens(): StoredTokens? = tokens
    override fun saveTokens(tokens: StoredTokens) {
        this.tokens = tokens
        saveCount++
    }
    override fun clear() {
        tokens = null
        clearCount++
    }
}

class BearerAuthConfigTest {

    private fun client(store: TokenStore, engine: MockEngine): HttpClient =
        HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; encodeDefaults = true })
            }
            installBearerAuth(tokenStore = store, baseUrl = "https://cupped.test")
        }

    @Test
    fun attachesBearerTokenWhenStored() = runTest {
        val store = FakeTokenStore(StoredTokens.single("token-abc"))
        var seenAuth: String? = null
        val engine = MockEngine { request ->
            seenAuth = request.headers[HttpHeaders.Authorization]
            respond("ok", HttpStatusCode.OK)
        }
        val http = client(store, engine)

        http.get("https://cupped.test/api/v1/sync/brew-logs/pull")

        assertEquals("Bearer token-abc", seenAuth)
    }

    @Test
    fun refreshesOn401ThenRetriesWithNewToken() = runTest {
        val store = FakeTokenStore(StoredTokens.single("stale"))
        val authHeaders = mutableListOf<String?>()
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            when {
                path.endsWith("/auth/refresh") ->
                    respond(
                        content = """{"token":"fresh"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                else -> {
                    val auth = request.headers[HttpHeaders.Authorization]
                    authHeaders += auth
                    if (auth == "Bearer stale") {
                        respond("nope", HttpStatusCode.Unauthorized)
                    } else {
                        respond("ok", HttpStatusCode.OK)
                    }
                }
            }
        }
        val http = client(store, engine)

        val response = http.get("https://cupped.test/api/v1/sync/brew-logs/pull")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("ok", response.bodyAsText())
        // The stale token was rejected, refresh produced "fresh", retry succeeded.
        assertTrue(authHeaders.contains("Bearer stale"))
        assertTrue(authHeaders.contains("Bearer fresh"))
        assertEquals(StoredTokens.single("fresh"), store.getTokens())
        assertEquals(1, store.saveCount)
    }

    @Test
    fun noTokenMeansNoAuthorizationHeader() = runTest {
        val store = FakeTokenStore(null)
        var seenAuth: String? = "unset"
        val engine = MockEngine { request ->
            seenAuth = request.headers[HttpHeaders.Authorization]
            respond("ok", HttpStatusCode.OK)
        }
        val http = client(store, engine)

        http.get("https://cupped.test/api/health")

        assertEquals(null, seenAuth)
    }

    /**
     * Headline reason the refresh Mutex exists: N parallel requests all 401 on
     * the stale token; only ONE refresh must fire and the store is saved once.
     */
    @Test
    fun concurrent401sDedupRefreshToOneCallAndOneSave() = runTest {
        val store = FakeTokenStore(StoredTokens.single("stale"))
        val countLock = Mutex()
        var refreshCalls = 0
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            when {
                path.endsWith("/auth/refresh") -> {
                    countLock.withLock { refreshCalls++ }
                    respond(
                        content = """{"token":"fresh"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(
                            HttpHeaders.ContentType,
                            ContentType.Application.Json.toString(),
                        ),
                    )
                }
                else -> {
                    val auth = request.headers[HttpHeaders.Authorization]
                    if (auth == "Bearer stale") respond("nope", HttpStatusCode.Unauthorized)
                    else respond("ok", HttpStatusCode.OK)
                }
            }
        }
        val http = client(store, engine)

        val n = 8
        val responses = (1..n).map {
            async { http.get("https://cupped.test/api/v1/sync/brew-logs/pull").status }
        }.awaitAll()

        responses.forEach { assertEquals(HttpStatusCode.OK, it) }
        assertEquals(1, refreshCalls, "refresh must fire exactly once for concurrent 401s")
        assertEquals(1, store.saveCount, "store must be saved exactly once")
        assertEquals(StoredTokens.single("fresh"), store.getTokens())
    }

    /**
     * Hard refresh failure: /auth/refresh returns 401 (expired session) ->
     * the store is cleared and the original call surfaces failure (fix #3).
     */
    @Test
    fun refreshFailureClearsStoreAndOriginalCallFails() = runTest {
        val store = FakeTokenStore(StoredTokens.single("stale"))
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            when {
                path.endsWith("/auth/refresh") -> respond("denied", HttpStatusCode.Unauthorized)
                else -> {
                    val auth = request.headers[HttpHeaders.Authorization]
                    if (auth == "Bearer stale") respond("nope", HttpStatusCode.Unauthorized)
                    else respond("ok", HttpStatusCode.OK)
                }
            }
        }
        val http = client(store, engine)

        val response = http.get("https://cupped.test/api/v1/sync/brew-logs/pull")

        // Refresh failed -> token cleared -> retry sends no/again-rejected token,
        // so the original call surfaces the 401 rather than looping forever.
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(store.clearCount >= 1, "store must be cleared on hard refresh failure")
        assertNull(store.getTokens())
    }

    /**
     * No-rotation guard: /auth/refresh returns 200 but the SAME token that just
     * 401'd -> treat as auth failure (clear + signed out), do not persist it.
     */
    @Test
    fun refreshReturningUnrotatedTokenClearsStore() = runTest {
        val store = FakeTokenStore(StoredTokens.single("stale"))
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            when {
                path.endsWith("/auth/refresh") -> respond(
                    content = """{"token":"stale"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(
                        HttpHeaders.ContentType,
                        ContentType.Application.Json.toString(),
                    ),
                )
                else -> {
                    val auth = request.headers[HttpHeaders.Authorization]
                    if (auth == "Bearer stale") respond("nope", HttpStatusCode.Unauthorized)
                    else respond("ok", HttpStatusCode.OK)
                }
            }
        }
        val http = client(store, engine)

        val response = http.get("https://cupped.test/api/v1/sync/brew-logs/pull")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(store.clearCount >= 1)
        assertNull(store.getTokens())
        assertEquals(0, store.saveCount, "an unrotated token must NOT be persisted")
    }

    /** Bearer must NOT ride along on the unauthenticated auth endpoints (fix #6). */
    @Test
    fun bearerNotAttachedToAuthEndpoints() = runTest {
        val store = FakeTokenStore(StoredTokens.single("token-abc"))
        val seen = mutableMapOf<String, String?>()
        val engine = MockEngine { request ->
            seen[request.url.encodedPath] = request.headers[HttpHeaders.Authorization]
            respond("ok", HttpStatusCode.OK)
        }
        val http = client(store, engine)

        http.get("https://cupped.test/api/v1/auth/magic-link")
        http.get("https://cupped.test/api/v1/auth/verify")
        http.get("https://cupped.test/api/v1/sync/brew-logs/pull")

        assertNull(seen["/api/v1/auth/magic-link"], "no bearer on magic-link")
        assertNull(seen["/api/v1/auth/verify"], "no bearer on verify")
        assertEquals("Bearer token-abc", seen["/api/v1/sync/brew-logs/pull"])
    }
}
