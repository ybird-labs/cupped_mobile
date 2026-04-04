package cafe.cupped.app.bridge

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Union of payloads that may cross the web/native bridge.
 *
 * Messages are intentionally transport-focused: they describe user-visible side
 * effects or state changes, not platform-specific implementation details.
 */
@Serializable
sealed class BridgeMessage {
    /** Auth state changed — no raw credentials, just the action */
    @Serializable
    @SerialName("auth_state_changed")
    data class AuthStateChanged(val action: AuthAction) : BridgeMessage()

    /** Navigate to a route */
    @Serializable
    @SerialName("navigate")
    data class Navigate(val path: String) : BridgeMessage()

    /** Trigger haptic feedback */
    @Serializable
    @SerialName("haptic")
    data class Haptic(val style: String) : BridgeMessage()

    /** Open native share sheet */
    @Serializable
    @SerialName("share")
    data class Share(val url: String, val title: String? = null) : BridgeMessage()

    /** Open native camera */
    @Serializable
    @SerialName("open_camera")
    data object OpenCamera : BridgeMessage()

    /** Photo upload completed */
    @Serializable
    @SerialName("photo_uploaded")
    data class PhotoUploaded(val url: String) : BridgeMessage()

    /** User cancelled the camera */
    @Serializable
    @SerialName("camera_cancelled")
    data object CameraCancelled : BridgeMessage()

    /** Console log from web */
    @Serializable
    @SerialName("console_log")
    data class ConsoleLog(val level: String, val message: String) : BridgeMessage()

    /** Register push notification token with the backend */
    @Serializable
    @SerialName("register_push_token")
    data class RegisterPushToken(val token: String, val platform: String) : BridgeMessage()

    /** Deep link received from the OS (universal link or custom URL scheme) */
    @Serializable
    @SerialName("deep_link_received")
    data class DeepLinkReceived(val url: String) : BridgeMessage()

    /** App lifecycle state change */
    @Serializable
    @SerialName("app_lifecycle")
    data class AppLifecycle(val state: AppState) : BridgeMessage()

    /** Network connectivity changed */
    @Serializable
    @SerialName("connectivity_changed")
    data class ConnectivityChanged(val status: ConnectivityStatus) : BridgeMessage()

    /** Result of a navigate request so the web side can fall back when native declined it. */
    @Serializable
    @SerialName("navigate_result")
    data class NavigateResult(val handled: Boolean) : BridgeMessage()

    /** Request the user's current location (one-shot). */
    @Serializable
    @SerialName("request_location")
    data class RequestLocation(
        val accuracy: LocationAccuracy = LocationAccuracy.BALANCED
    ) : BridgeMessage()

    /** Location successfully obtained. */
    @Serializable
    @SerialName("location_result")
    data class LocationResult(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Double,
        val altitude: Double? = null,
        val timestamp: Long
    ) : BridgeMessage()

    /** User denied location permission. */
    @Serializable
    @SerialName("location_denied")
    data object LocationDenied : BridgeMessage()

    /** Generic error response for request/response pairs that cannot complete successfully. */
    @Serializable
    @SerialName("error")
    data class Error(val code: String, val message: String) : BridgeMessage()
}
