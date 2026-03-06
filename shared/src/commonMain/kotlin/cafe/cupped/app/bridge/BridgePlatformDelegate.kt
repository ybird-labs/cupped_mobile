package cafe.cupped.app.bridge

import cafe.cupped.app.navigation.Route

interface BridgePlatformDelegate {
    fun sendToWeb(json: String)

    fun handleNavigate(route: Route): Boolean

    fun handleHaptic(style: String)

    fun handleShare(url: String, title: String?)

    fun handleOpenCamera(requestId: String)

    fun handleConsoleLog(level: String, message: String)
}
