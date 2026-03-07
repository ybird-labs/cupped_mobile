package cafe.cupped.app.bridge

import cafe.cupped.app.navigation.Route

/**
 * Native-side hooks invoked after the bridge decodes a web message.
 *
 * Implementations own the platform effects. The bridge layer itself stays pure:
 * it decodes, routes, and reports whether native handling succeeded.
 */
interface BridgePlatformDelegate {
    /** Sends a fully encoded envelope back to the web runtime. */
    fun sendToWeb(json: String)

    /**
     * Attempts native navigation for the resolved route.
     *
     * Return `true` only when native code consumed the route. Returning `false`
     * tells the caller to keep the navigation in the web layer.
     */
    fun handleNavigate(route: Route): Boolean

    /** Performs best-effort haptic feedback for a semantic style key. */
    fun handleHaptic(style: String)

    /** Opens the platform share sheet for a fully qualified URL. */
    fun handleShare(url: String, title: String?)

    /** Starts a camera flow that must eventually reply using the original request ID. */
    fun handleOpenCamera(requestId: String)

    /** Mirrors web console output into native logging without affecting app control flow. */
    fun handleConsoleLog(level: String, message: String)
}
