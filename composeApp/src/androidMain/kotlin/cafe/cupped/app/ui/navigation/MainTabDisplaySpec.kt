package cafe.cupped.app.ui.navigation

import cafe.cupped.app.designsystem.icons.AppIcon as DesignIcon
import cafe.cupped.app.navigation.shell.MainTabId

internal data class MainTabDisplaySpec(
    val title: String,
    val testTagSuffix: String,
    val activeIcon: DesignIcon,
    val inactiveIcon: DesignIcon,
)

internal fun MainTabId.displaySpec(): MainTabDisplaySpec = when (this) {
    MainTabId.Feed -> MainTabDisplaySpec(
        title = "Feed",
        testTagSuffix = "feed",
        activeIcon = DesignIcon.HomeActive,
        inactiveIcon = DesignIcon.Home,
    )
    MainTabId.Discover -> MainTabDisplaySpec(
        title = "Discover",
        testTagSuffix = "discover",
        activeIcon = DesignIcon.DiscoverActive,
        inactiveIcon = DesignIcon.Discover,
    )
    MainTabId.Community -> MainTabDisplaySpec(
        title = "Community",
        testTagSuffix = "community",
        activeIcon = DesignIcon.CommunityActive,
        inactiveIcon = DesignIcon.Community,
    )
    MainTabId.Profile -> MainTabDisplaySpec(
        title = "Profile",
        testTagSuffix = "profile",
        activeIcon = DesignIcon.ProfileActive,
        inactiveIcon = DesignIcon.Profile,
    )
}
