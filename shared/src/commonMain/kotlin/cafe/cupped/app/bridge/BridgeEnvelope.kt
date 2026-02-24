package cafe.cupped.app.bridge

import kotlinx.serialization.Serializable

/**
 * Correlation envelope wrapping a [BridgeMessage].
 *
 * Every message gets a unique [id]. Request/response pairs are correlated
 * by setting [replyTo] to the [id] of the original request.
 *
 * Example flow:
 *   Web -> Native:  { id: "a1", message: { type: "open_camera" } }
 *   Native -> Web:  { id: "b2", replyTo: "a1", message: { type: "photo_uploaded", url: "..." } }
 *   or on cancel:   { id: "b3", replyTo: "a1", message: { type: "camera_cancelled" } }
 *   or on failure:  { id: "b4", replyTo: "a1", message: { type: "error", code: "...", message: "..." } }
 */
@Serializable
data class BridgeEnvelope(
    val id: String,
    val replyTo: String? = null,
    val message: BridgeMessage
)
