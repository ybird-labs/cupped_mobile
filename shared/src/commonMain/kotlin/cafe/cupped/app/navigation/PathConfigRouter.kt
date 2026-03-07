package cafe.cupped.app.navigation

/**
 * Resolves web paths into native [Route] values.
 *
 * The router strips query strings and fragments before matching so route
 * selection depends only on the path shape. Mapping order is significant:
 * the first matching pattern wins.
 *
 * When no mapping matches, callers must fall back to [Route.Web] so the
 * original path can still load inside the web experience instead of failing
 * navigation outright.
 */
class PathConfigRouter(private val config: PathConfig = PathConfig.default()) {

    /**
     * Resolves a path using the configured mapping precedence.
     *
     * Trailing slashes are normalized away for matching, but the original
     * `path` is preserved in [Route.Web] so the fallback keeps the exact web
     * destination the user requested.
     */
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

    /**
     * Matches a path against a colon-parameter pattern such as `/posts/:id`.
     *
     * Patterns must match segment-for-segment. This intentionally does not do
     * prefix matching because partial matches would make route precedence
     * unpredictable.
     */
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

    /**
     * Converts a matched route name plus extracted params into a typed route.
     *
     * Parameterized routes degrade to an empty ID when config and route names
     * drift out of sync. That fallback keeps parsing total; callers that depend
     * on a non-empty ID should validate before use.
     */
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
