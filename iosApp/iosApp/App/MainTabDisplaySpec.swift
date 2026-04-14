import Foundation
import Shared

struct MainTabDisplaySpec: Hashable {
    let id: MainTabId
    let title: String
    let inactiveIcon: AppIcon
    let activeIcon: AppIcon
    let identifierSuffix: String

    static func orderedTabs(locale: Locale) -> [MainTabDisplaySpec] {
        [
            MainTabDisplaySpec(
                id: .feed,
                title: String(localized: "Feed", locale: locale),
                inactiveIcon: .home,
                activeIcon: .homeActive,
                identifierSuffix: "feed"
            ),
            MainTabDisplaySpec(
                id: .discover,
                title: String(localized: "Discover", locale: locale),
                inactiveIcon: .discover,
                activeIcon: .discoverActive,
                identifierSuffix: "discover"
            ),
            MainTabDisplaySpec(
                id: .community,
                title: String(localized: "Community", locale: locale),
                inactiveIcon: .community,
                activeIcon: .communityActive,
                identifierSuffix: "community"
            ),
            MainTabDisplaySpec(
                id: .profile,
                title: String(localized: "Profile", locale: locale),
                inactiveIcon: .profile,
                activeIcon: .profileActive,
                identifierSuffix: "profile"
            ),
        ]
    }
}
