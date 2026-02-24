package cafe.cupped.app.bridge

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

object BridgeCodec {
    private val json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** Encode a single message to JSON */
    fun encode(message: BridgeMessage): String = json.encodeToString(message)

    /** Decode a single message from JSON — throws on unknown types */
    fun decode(jsonString: String): BridgeMessage = json.decodeFromString(jsonString)

    /**
     * Decode a single message from JSON — returns null on unknown or malformed types.
     * Use this at the bridge boundary for forward compatibility (e.g., server deploys
     * a new message type before the app updates).
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

    /** Decode an envelope from JSON — returns null on unknown or malformed types */
    fun decodeEnvelopeSafe(jsonString: String): BridgeEnvelope? = try {
        json.decodeFromString(jsonString)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}
