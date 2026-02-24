package cafe.cupped.app

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

/** True when running a debug build. Used to gate verbose logging. */
expect val isDebug: Boolean
