package cafe.cupped.app.navigation

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Named mapping from a path pattern to a route identifier understood by [PathConfigRouter]. */
@Serializable
data class PathMapping(val pattern: String, val route: String)

@Serializable
data class PathConfig(val mappings: List<PathMapping>) {
    companion object {
        /**
         * Default path config compiled into the binary.
         *
         * Keeping this in common code makes native route resolution deterministic
         * even before any remote or web-owned config has loaded.
         */
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

        /** Parses the built-in route table used by native/web navigation handoff. */
        fun default(): PathConfig = json.decodeFromString(DEFAULT_CONFIG_JSON)
    }
}
