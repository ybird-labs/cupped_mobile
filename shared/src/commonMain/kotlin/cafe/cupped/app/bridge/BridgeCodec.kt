package cafe.cupped.app.bridge

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

/**
 * JSON codec for the web/native bridge protocol.
 *
 * The bridge uses a stable `type` discriminator so Swift, Kotlin, and web code
 * can evolve independently while sharing the same envelope shape.
 */
object BridgeCodec {
    private val json = Json {
        classDiscriminator = "type"
        // Bridge payloads may arrive from a newer web build before the app is
        // updated. Unknown fields are ignored so additive changes stay forward
        // compatible at the protocol boundary.
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** Encode a single message to JSON */
    fun encode(message: BridgeMessage): String = json.encodeToString(message)

    /** Decode a single message from JSON — throws on unknown types */
    fun decode(jsonString: String): BridgeMessage = json.decodeFromString(jsonString)

    /**
     * Decode a single message from JSON — returns null on unknown or malformed types.
     * Use this only at the trust boundary where malformed input should be dropped
     * instead of crashing the host app. Once a payload is accepted, downstream code
     * should operate on typed messages rather than re-decoding strings.
     */
    fun decodeSafe(jsonString: String): BridgeMessage? = try {
        json.decodeFromString(jsonString)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    /** Encode an envelope (message + correlation ID) to JSON */
    fun encodeEnvelope(envelope: BridgeEnvelope): String = json.encodeToString(envelope)

    /** Decode an envelope from JSON — throws on unknown types */
    fun decodeEnvelope(jsonString: String): BridgeEnvelope = json.decodeFromString(jsonString)

    /**
     * Decode an envelope from JSON — returns null on unknown or malformed types.
     *
     * Envelopes are where request/response correlation lives, so failures here mean
     * the entire message is unusable and must be ignored as a unit.
     */
    fun decodeEnvelopeSafe(jsonString: String): BridgeEnvelope? = try {
        json.decodeFromString(jsonString)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}
