package cafe.cupped.app.ui.navigation

import cafe.cupped.app.designsystem.icons.AppIcon as DesignIcon
import cafe.cupped.app.navigation.shell.MainTabId

internal data class MainTabDisplaySpec(
    val id: MainTabId,
    val title: String,
    val testTagSuffix: String,
    val activeIcon: DesignIcon,
    val inactiveIcon: DesignIcon,
) {
    companion object {
        val orderedTabs = listOf(
            MainTabId.Feed.displaySpec(),
            MainTabId.Discover.displaySpec(),
            MainTabId.Community.displaySpec(),
            MainTabId.Profile.displaySpec(),
        )
    }
}

internal fun MainTabId.displaySpec(): MainTabDisplaySpec = when (this) {
    MainTabId.Feed -> MainTabDisplaySpec(
        id = MainTabId.Feed,
        title = "Feed",
        testTagSuffix = "feed",
        activeIcon = DesignIcon.HomeActive,
        inactiveIcon = DesignIcon.Home,
    )
    MainTabId.Discover -> MainTabDisplaySpec(
        id = MainTabId.Discover,
        title = "Discover",
        testTagSuffix = "discover",
        activeIcon = DesignIcon.DiscoverActive,
        inactiveIcon = DesignIcon.Discover,
    )
    MainTabId.Community -> MainTabDisplaySpec(
        id = MainTabId.Community,
        title = "Community",
        testTagSuffix = "community",
        activeIcon = DesignIcon.CommunityActive,
        inactiveIcon = DesignIcon.Community,
    )
    MainTabId.Profile -> MainTabDisplaySpec(
        id = MainTabId.Profile,
        title = "Profile",
        testTagSuffix = "profile",
        activeIcon = DesignIcon.ProfileActive,
        inactiveIcon = DesignIcon.Profile,
    )
}
