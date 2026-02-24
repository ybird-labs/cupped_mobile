package cafe.cupped.app.bridge

import kotlinx.serialization.Serializable

@Serializable
enum class AuthAction {
    SIGNED_IN,
    SIGNED_OUT
}

@Serializable
enum class AppState {
    FOREGROUNDED,
    BACKGROUNDED,
    WILL_TERMINATE
}

@Serializable
enum class ConnectivityStatus {
    ONLINE,
    OFFLINE,
    CELLULAR,
    WIFI
}
