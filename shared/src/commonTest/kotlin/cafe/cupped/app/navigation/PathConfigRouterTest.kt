package cafe.cupped.app.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PathConfigRouterTest {
    private val router = PathConfigRouter()

    @Test fun rootResolvesToFeed() = assertEquals(Route.Feed, router.resolve(AppPaths.root))
    @Test fun feedPath() = assertEquals(Route.Feed, router.resolve(AppPaths.feed))
    @Test fun discoverPath() = assertEquals(Route.Discover, router.resolve(AppPaths.discover))
    @Test fun profilePath() = assertEquals(Route.Profile, router.resolve(AppPaths.profile))

    @Test fun postWithId() {
        val route = router.resolve("/posts/abc-123")
        assertIs<Route.Post>(route)
        assertEquals("abc-123", route.id)
    }

    @Test fun userProfileWithId() {
        val route = router.resolve("/users/user-456")
        assertIs<Route.UserProfile>(route)
        assertEquals("user-456", route.id)
    }

    @Test fun cafeWithId() {
        val route = router.resolve("/cafes/cafe-789")
        assertIs<Route.Cafe>(route)
        assertEquals("cafe-789", route.id)
    }

    @Test fun unknownPathFallsToWeb() {
        val route = router.resolve("/unknown/deep/path")
        assertIs<Route.Web>(route)
        assertEquals("/unknown/deep/path", route.path)
    }

    @Test fun queryParamsStripped() {
        assertEquals(Route.Feed, router.resolve(AppPaths.feed + "?tab=latest"))
    }

    @Test fun fragmentStripped() {
        assertEquals(Route.Feed, router.resolve(AppPaths.feed + "#top"))
    }
}
