package cafe.cupped.app.ui.navigation

import androidx.annotation.StringRes
import cafe.cupped.app.R
import cafe.cupped.app.designsystem.icons.AppIcon as DesignIcon
import cafe.cupped.app.navigation.shell.MainTabId

internal data class MainTabDisplaySpec(
    val id: MainTabId,
    @StringRes val titleRes: Int,
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
        titleRes = R.string.tab_feed,
        testTagSuffix = "feed",
        activeIcon = DesignIcon.HomeActive,
        inactiveIcon = DesignIcon.Home,
    )
    MainTabId.Discover -> MainTabDisplaySpec(
        id = MainTabId.Discover,
        titleRes = R.string.tab_discover,
        testTagSuffix = "discover",
        activeIcon = DesignIcon.DiscoverActive,
        inactiveIcon = DesignIcon.Discover,
    )
    MainTabId.Community -> MainTabDisplaySpec(
        id = MainTabId.Community,
        titleRes = R.string.tab_community,
        testTagSuffix = "community",
        activeIcon = DesignIcon.CommunityActive,
        inactiveIcon = DesignIcon.Community,
    )
    MainTabId.Profile -> MainTabDisplaySpec(
        id = MainTabId.Profile,
        titleRes = R.string.tab_profile,
        testTagSuffix = "profile",
        activeIcon = DesignIcon.ProfileActive,
        inactiveIcon = DesignIcon.Profile,
    )
}
