import Shared

struct MainTabDisplaySpec: Hashable {
    let id: MainTabId
    let title: String
    let inactiveIcon: AppIcon
    let activeIcon: AppIcon
    let identifierSuffix: String

    static let orderedTabs: [MainTabDisplaySpec] = [
        MainTabDisplaySpec(
            id: .feed,
            title: String(localized: "Feed"),
            inactiveIcon: .home,
            activeIcon: .homeActive,
            identifierSuffix: "feed"
        ),
        MainTabDisplaySpec(
            id: .discover,
            title: String(localized: "Discover"),
            inactiveIcon: .discover,
            activeIcon: .discoverActive,
            identifierSuffix: "discover"
        ),
        MainTabDisplaySpec(
            id: .community,
            title: String(localized: "Community"),
            inactiveIcon: .community,
            activeIcon: .communityActive,
            identifierSuffix: "community"
        ),
        MainTabDisplaySpec(
            id: .profile,
            title: String(localized: "Profile"),
            inactiveIcon: .profile,
            activeIcon: .profileActive,
            identifierSuffix: "profile"
        ),
    ]
}
