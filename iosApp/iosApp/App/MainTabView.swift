import SwiftUI

enum Tab: String, CaseIterable {
    case feed, discover, log, community, profile

    var title: String {
        rawValue.capitalized
    }

    var icon: String {
        switch self {
        case .feed: "house"
        case .discover: "safari"
        case .log: "plus.circle"
        case .community: "person.2"
        case .profile: "person.crop.circle"
        }
    }

    var filledIcon: String {
        switch self {
        case .feed: "house.fill"
        case .discover: "safari.fill"
        case .log: "plus.circle.fill"
        case .community: "person.2.fill"
        case .profile: "person.crop.circle.fill"
        }
    }
}

struct MainTabView: View {
    @State private var selectedTab: Tab = .feed

    var body: some View {
        TabView(selection: $selectedTab) {
            ForEach(Tab.allCases, id: \.self) { tab in
                tabContent(for: tab)
                    .tabItem {
                        Image(systemName: selectedTab == tab ? tab.filledIcon : tab.icon)
                        Text(tab.title)
                    }
                    .tag(tab)
            }
        }
        // Global tint set at WindowGroup root in iOSApp.swift
    }

    @ViewBuilder
    private func tabContent(for tab: Tab) -> some View {
        switch tab {
        case .feed: FeedView()
        case .discover: DiscoverView()
        case .log: LogView()
        case .community: CommunityView()
        case .profile: ProfileView()
        }
    }
}

#Preview {
    MainTabView()
}
