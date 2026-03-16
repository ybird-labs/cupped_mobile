import SwiftUI

private enum AppTab: String, CaseIterable {
    case feed
    case profile

    var title: String {
        rawValue.capitalized
    }

    var icon: String {
        switch self {
        case .feed:
            "house"
        case .profile:
            "person.crop.circle"
        }
    }

    var selectedIcon: String {
        switch self {
        case .feed:
            "house.fill"
        case .profile:
            "person.crop.circle.fill"
        }
    }
}

struct MainTabView: View {
    @State private var selectedTab: AppTab = .feed

    var body: some View {
        TabView(selection: $selectedTab) {
            ForEach(AppTab.allCases, id: \.self) { tab in
                tabContent(for: tab)
                    .tabItem {
                        Image(systemName: selectedTab == tab ? tab.selectedIcon : tab.icon)
                        Text(tab.title)
                    }
                    .tag(tab)
            }
        }
    }

    @ViewBuilder
    private func tabContent(for tab: AppTab) -> some View {
        switch tab {
        case .feed:
            FeedView()
        case .profile:
            ProfileView()
        }
    }
}

#Preview {
    MainTabView()
        .environment(AuthCoordinator())
}
