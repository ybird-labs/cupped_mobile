package cafe.cupped.app.network

import cafe.cupped.app.isDebug
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.github.aakira.napier.Napier

/** Headers whose values must never appear in logs. */
private val SENSITIVE_HEADERS = setOf("authorization", "cookie", "set-cookie")

/**
 * Installs HTTP logging only in debug builds.
 * Sanitizes Authorization, Cookie, and Set-Cookie header values.
 */
fun HttpClientConfig<*>.configureHttpLogging() {
    if (!isDebug) return

    install(Logging) {
        level = LogLevel.HEADERS
        logger = object : Logger {
            override fun log(message: String) {
                Napier.d(tag = "HTTP", message = message)
            }
        }
        sanitizeHeader { name ->
            name.lowercase() in SENSITIVE_HEADERS
        }
    }
}
