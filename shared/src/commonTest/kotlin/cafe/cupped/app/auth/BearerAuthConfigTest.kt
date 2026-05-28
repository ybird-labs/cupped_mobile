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
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** In-memory [TokenStore] for tests. */
private class FakeTokenStore(initial: StoredTokens? = null) : TokenStore {
    private var tokens: StoredTokens? = initial
    var saveCount = 0
        private set

    override fun getTokens(): StoredTokens? = tokens
    override fun saveTokens(tokens: StoredTokens) {
        this.tokens = tokens
        saveCount++
    }
    override fun clear() { tokens = null }
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
}
