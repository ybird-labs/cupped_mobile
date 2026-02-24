package cafe.cupped.app.navigation

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PathMapping(val pattern: String, val route: String)

@Serializable
data class PathConfig(val mappings: List<PathMapping>) {
    companion object {
        /** Default path config — compiled into the binary, no resource loading needed */
        const val DEFAULT_CONFIG_JSON = """
        {
            "mappings": [
                {"pattern": "/", "route": "feed"},
                {"pattern": "/feed", "route": "feed"},
                {"pattern": "/discover", "route": "discover"},
                {"pattern": "/log", "route": "log"},
                {"pattern": "/community", "route": "community"},
                {"pattern": "/profile", "route": "profile"},
                {"pattern": "/posts/:id", "route": "post"},
                {"pattern": "/users/:id", "route": "user_profile"},
                {"pattern": "/cafes/:id", "route": "cafe"},
                {"pattern": "/login", "route": "login"},
                {"pattern": "/register", "route": "register"}
            ]
        }
        """

        private val json = Json { ignoreUnknownKeys = true }

        fun default(): PathConfig = json.decodeFromString(DEFAULT_CONFIG_JSON)
    }
}
