package cafe.cupped.app.bridge

import cafe.cupped.app.navigation.PathConfigRouter

class BridgeDispatcher(
    val router: PathConfigRouter,
    val delegate: BridgePlatformDelegate
)
