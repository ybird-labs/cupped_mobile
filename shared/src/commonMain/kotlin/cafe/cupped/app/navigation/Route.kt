package cafe.cupped.app.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Route {
    /** Tab roots */
    @Serializable data object Feed : Route()
    @Serializable data object Discover : Route()
    @Serializable data object Log : Route()
    @Serializable data object Community : Route()
    @Serializable data object Profile : Route()

    /** Content routes */
    @Serializable data class Post(val id: String) : Route()
    @Serializable data class UserProfile(val id: String) : Route()
    @Serializable data class Cafe(val id: String) : Route()

    /** Auth routes */
    @Serializable data object Login : Route()
    @Serializable data object Register : Route()

    /** Fallback — load as WebView URL */
    @Serializable data class Web(val path: String) : Route()
}
