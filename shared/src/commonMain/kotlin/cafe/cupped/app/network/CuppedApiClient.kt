package cafe.cupped.app.network

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import kotlin.coroutines.cancellation.CancellationException

class CuppedApiClient(
    private val httpClient: HttpClient,
    private val baseUrl: String
) {
    /**
     * Returns true only if the server responds with a 2xx status code.
     * Does NOT return true on connection errors, timeouts, or non-2xx responses.
     */
    suspend fun healthCheck(): Boolean {
        return try {
            val response: HttpResponse = httpClient.get("$baseUrl/api/health")
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
}
