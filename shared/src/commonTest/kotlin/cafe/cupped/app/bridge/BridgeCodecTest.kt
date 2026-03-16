package cafe.cupped.app.bridge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BridgeCodecTest {

    // ── Message round-trips (one per variant) ──────────────────────────

    @Test
    fun authStateChangedSignedIn() {
        val msg = BridgeMessage.AuthStateChanged(AuthAction.SIGNED_IN)
        val json = BridgeCodec.encode(msg)
        val decoded = BridgeCodec.decode(json)
        assertEquals(msg, decoded)
    }

    @Test
    fun authStateChangedSignedOut() {
        val msg = BridgeMessage.AuthStateChanged(AuthAction.SIGNED_OUT)
        val json = BridgeCodec.encode(msg)
        val decoded = BridgeCodec.decode(json)
        assertEquals(msg, decoded)
    }

    @Test
    fun navigate() {
        val msg = BridgeMessage.Navigate("/feed")
        val json = BridgeCodec.encode(msg)
        val decoded = BridgeCodec.decode(json)
        assertEquals(msg, decoded)
    }

    @Test
    fun haptic() {
        val msg = BridgeMessage.Haptic("impact")
        val json = BridgeCodec.encode(msg)
        val decoded = BridgeCodec.decode(json)
        assertEquals(msg, decoded)
    }

    @Test
    fun shareWithTitle() {
        val msg = BridgeMessage.Share("https://cupped.cafe/post/1", title = "My Coffee")
        val json = BridgeCodec.encode(msg)
        val decoded = BridgeCodec.decode(json)
        assertEquals(msg, decoded)
    }

    @Test
    fun shareWithoutTitle() {
        val msg = BridgeMessage.Share("https://cupped.cafe/post/1")
        val json = BridgeCodec.encode(msg)
        val decoded = BridgeCodec.decode(json)
        assertEquals(msg, decoded)
    }

    @Test
    fun openCamera() {
        val msg = BridgeMessage.OpenCamera
        val json = BridgeCodec.encode(msg)
        val decoded = BridgeCodec.decode(json)
        assertEquals(msg, decoded)
    }

    @Test
    fun photoUploaded() {
        val msg = BridgeMessage.PhotoUploaded("https://s3.example.com/photo.jpg")
        val json = BridgeCodec.encode(msg)
        val decoded = BridgeCodec.decode(json)
        assertEquals(msg, decoded)
    }

    @Test
    fun cameraCancelled() {
        val msg = BridgeMessage.CameraCancelled
        val json = BridgeCodec.encode(msg)
        val decoded = BridgeCodec.decode(json)
        assertEquals(msg, decoded)
    }

    @Test
    fun consoleLog() {
        val msg = BridgeMessage.ConsoleLog("warn", "LiveView socket disconnected")
        val json = BridgeCodec.encode(msg)
        val decoded = BridgeCodec.decode(json)
        assertEquals(msg, decoded)
    }

    @Test
    fun registerPushToken() {
        val msg = BridgeMessage.RegisterPushToken(token = "abc123-apns-token", platform = "ios")
        val json = BridgeCodec.encode(msg)
        val decoded = BridgeCodec.decode(json)
        assertEquals(msg, decoded)
    }

    @Test
    fun deepLinkReceived() {
        val msg = BridgeMessage.DeepLinkReceived("cupped://posts/abc-123")
        val json = BridgeCodec.encode(msg)
        val decoded = BridgeCodec.decode(json)
        assertEquals(msg, decoded)
    }

    @Test
    fun appLifecycleForegrounded() {
        val msg = BridgeMessage.AppLifecycle(AppState.FOREGROUNDED)
        val json = BridgeCodec.encode(msg)
        val decoded = BridgeCodec.decode(json)
        assertEquals(msg, decoded)
    }

    @Test
    fun appLifecycleBackgrounded() {
        val msg = BridgeMessage.AppLifecycle(AppState.BACKGROUNDED)
        val json = BridgeCodec.encode(msg)
        val decoded = BridgeCodec.decode(json)
        assertEquals(msg, decoded)
    }

    @Test
    fun connectivityChangedWifi() {
        val msg = BridgeMessage.ConnectivityChanged(ConnectivityStatus.WIFI)
        val json = BridgeCodec.encode(msg)
        val decoded = BridgeCodec.decode(json)
        assertEquals(msg, decoded)
    }

    @Test
    fun connectivityChangedOffline() {
        val msg = BridgeMessage.ConnectivityChanged(ConnectivityStatus.OFFLINE)
        val json = BridgeCodec.encode(msg)
        val decoded = BridgeCodec.decode(json)
        assertEquals(msg, decoded)
    }

    @Test
    fun navigateResultHandled() {
        val msg = BridgeMessage.NavigateResult(handled = true)
        val json = BridgeCodec.encode(msg)
        val decoded = BridgeCodec.decode(json)
        assertEquals(msg, decoded)
        assertTrue(json.contains("\"type\":\"navigate_result\""))
    }

    @Test
    fun navigateResultUnhandled() {
        val msg = BridgeMessage.NavigateResult(handled = false)
        val json = BridgeCodec.encode(msg)
        val decoded = BridgeCodec.decode(json)
        assertEquals(msg, decoded)
    }

    @Test
    fun errorMessage() {
        val msg = BridgeMessage.Error(code = "camera_failed", message = "No camera access")
        val json = BridgeCodec.encode(msg)
        val decoded = BridgeCodec.decode(json)
        assertEquals(msg, decoded)
    }

    // ── Envelope round-trips ───────────────────────────────────────────

    @Test
    fun envelopeRoundTrip() {
        val envelope = BridgeEnvelope(
            id = "msg-001",
            message = BridgeMessage.OpenCamera
        )
        val json = BridgeCodec.encodeEnvelope(envelope)
        val decoded = BridgeCodec.decodeEnvelope(json)
        assertEquals(envelope, decoded)
        assertNull(decoded.replyTo)
    }

    @Test
    fun envelopeWithReplyTo() {
        val envelope = BridgeEnvelope(
            id = "msg-002",
            replyTo = "msg-001",
            message = BridgeMessage.PhotoUploaded("https://s3.example.com/photo.jpg")
        )
        val json = BridgeCodec.encodeEnvelope(envelope)
        val decoded = BridgeCodec.decodeEnvelope(json)
        assertEquals(envelope, decoded)
        assertEquals("msg-001", decoded.replyTo)
        assertIs<BridgeMessage.PhotoUploaded>(decoded.message)
    }

    @Test
    fun envelopeErrorReply() {
        val envelope = BridgeEnvelope(
            id = "msg-003",
            replyTo = "msg-001",
            message = BridgeMessage.Error(code = "camera_denied", message = "Permission denied")
        )
        val json = BridgeCodec.encodeEnvelope(envelope)
        val decoded = BridgeCodec.decodeEnvelope(json)
        assertEquals("msg-001", decoded.replyTo)
        assertIs<BridgeMessage.Error>(decoded.message)
        val error = decoded.message
        assertIs<BridgeMessage.Error>(error)
        assertEquals("camera_denied", error.code)
    }

    // ── decodeSafe / forward compatibility ─────────────────────────────

    @Test
    fun decodeSafeReturnsNullForUnknownType() {
        val json = """{"type":"some_future_message","data":"hello"}"""
        assertNull(BridgeCodec.decodeSafe(json))
    }

    @Test
    fun decodeSafeReturnsNullForMalformedJson() {
        assertNull(BridgeCodec.decodeSafe("not json at all"))
    }

    @Test
    fun decodeSafeReturnsMessageForValidJson() {
        val msg = BridgeMessage.Haptic("impact")
        val json = BridgeCodec.encode(msg)
        val decoded = BridgeCodec.decodeSafe(json)
        assertNotNull(decoded)
        assertEquals(msg, decoded)
    }

    @Test
    fun decodeEnvelopeSafeReturnsNullForUnknownMessage() {
        val json = """{"id":"x","message":{"type":"unknown_thing"}}"""
        assertNull(BridgeCodec.decodeEnvelopeSafe(json))
    }

    // ── Type discriminator verification ────────────────────────────────

    @Test
    fun jsonContainsTypeDiscriminator() {
        val json = BridgeCodec.encode(BridgeMessage.AuthStateChanged(AuthAction.SIGNED_IN))
        assertTrue(json.contains(""""type":"auth_state_changed""""))
    }
}
