package cafe.cupped.app.navigation

class PathConfigRouter(private val config: PathConfig = PathConfig.default()) {

    fun resolve(path: String): Route {
        val cleanPath = path.split("?")[0].split("#")[0]
        val normalizedPath = cleanPath.trimEnd('/')
        for (mapping in config.mappings) {
            val match = matchPattern(mapping.pattern, normalizedPath)
            if (match != null) {
                return toRoute(mapping.route, match)
            }
        }
        return Route.Web(path)
    }

    private fun matchPattern(pattern: String, path: String): Map<String, String>? {
        val patternParts = pattern.trimEnd('/').split("/")
        val pathParts = path.split("/")
        if (patternParts.size != pathParts.size) return null

        val params = mutableMapOf<String, String>()
        for (i in patternParts.indices) {
            if (patternParts[i].startsWith(":")) {
                params[patternParts[i].removePrefix(":")] = pathParts[i]
            } else if (patternParts[i] != pathParts[i]) {
                return null
            }
        }
        return params
    }

    private fun toRoute(routeName: String, params: Map<String, String>): Route {
        return when (routeName) {
            "feed" -> Route.Feed
            "discover" -> Route.Discover
            "log" -> Route.Log
            "community" -> Route.Community
            "profile" -> Route.Profile
            "post" -> Route.Post(params["id"] ?: "")
            "user_profile" -> Route.UserProfile(params["id"] ?: "")
            "cafe" -> Route.Cafe(params["id"] ?: "")
            "login" -> Route.Login
            "register" -> Route.Register
            else -> Route.Web("/$routeName")
        }
    }
}
