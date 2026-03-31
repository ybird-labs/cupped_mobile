package cafe.cupped.app.navigation

import kotlinx.serialization.Serializable

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
        /** Parses the built-in route table used by native/web navigation handoff. */
        fun default(): PathConfig = PathConfig(
            mappings = listOf(
                PathMapping(AppPaths.root, "/mobile/feed"),
                PathMapping(AppPaths.feed, "/mobile/feed"),
                PathMapping(AppPaths.discover, "/mobile/discover"),
                PathMapping(AppPaths.log, "log"),
                PathMapping(AppPaths.community, "/mobile/community"),
                PathMapping(AppPaths.profile, "profile"),
                PathMapping("/posts/:id", "post"),
                PathMapping("/users/:id", "user_profile"),
                PathMapping("/cafes/:id", "cafe"),
                PathMapping(AppPaths.login, "login"),
                PathMapping(AppPaths.register, "register")
            )
        )
    }
}
