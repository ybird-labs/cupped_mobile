package cafe.cupped.app.navigation.shell

import cafe.cupped.app.navigation.Route
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MainShellRoutesTest {

    @Test
    fun tabOrderMatchesEnumDeclaration() {
        assertContentEquals(
            expected = listOf(
                MainTabId.Feed,
                MainTabId.Discover,
                MainTabId.Community,
                MainTabId.Profile,
            ),
            actual = MainTabId.entries.toList(),
        )
    }

    @Test
    fun mainRoutesResolveToMatchingTabs() {
        assertEquals(MainTabId.Feed, Route.Feed.mainTabOrNull())
        assertEquals(MainTabId.Discover, Route.Discover.mainTabOrNull())
        assertEquals(MainTabId.Community, Route.Community.mainTabOrNull())
        assertEquals(MainTabId.Profile, Route.Profile.mainTabOrNull())
    }

    @Test
    fun logRouteResolvesToCenterActionOnly() {
        assertEquals(MainShellAction.Log, Route.Log.shellActionOrNull())
        assertNull(Route.Log.mainTabOrNull())
    }

    @Test
    fun unknownRoutesDoNotResolveToShellState() {
        assertNull(Route.Web("/unknown").mainTabOrNull())
        assertNull(Route.Web("/unknown").shellActionOrNull())
        assertNull(Route.Post("post-1").mainTabOrNull())
        assertNull(Route.Post("post-1").shellActionOrNull())
    }
}
