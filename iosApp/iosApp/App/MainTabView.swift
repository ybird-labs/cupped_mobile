import SwiftUI
import Shared

private struct BarHeightKey: PreferenceKey {
    static var defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

private enum PresentedSheet: Identifiable {
    case log

    var id: Self { self }
}

struct MainTabView: View {
    @State private var selectedTab: MainTabId = .feed
    @State private var presentedSheet: PresentedSheet?
    @State private var barHeight: CGFloat = 0
    @Namespace private var tabNamespace

    private var leftTabs: [MainTabDisplaySpec] {
        Array(MainTabDisplaySpec.orderedTabs.prefix(2))
    }

    private var rightTabs: [MainTabDisplaySpec] {
        Array(MainTabDisplaySpec.orderedTabs.suffix(2))
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            tabContent(for: selectedTab)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .padding(.bottom, barHeight)

            barView
        }
        .animation(.cuppedSpring, value: selectedTab)
        .sheet(item: $presentedSheet) { sheet in
            switch sheet {
            case .log:
                LogView()
            }
        }
    }

    @ViewBuilder
    private func tabContent(for tab: MainTabId) -> some View {
        switch tab {
        case .feed:
            FeedView()
        case .discover:
            DiscoverView()
        case .community:
            CommunityView()
        case .profile:
            ProfileView()
        }
    }

    private var barView: some View {
        VStack(spacing: 0) {
            topBorder
            barContent
        }
        .background(barBackground)
        .background(
            GeometryReader { geo in
                Color.clear.preference(key: BarHeightKey.self, value: geo.size.height)
            }
        )
        .onPreferenceChange(BarHeightKey.self) { newBarHeight in
            guard barHeight != newBarHeight else { return }
            barHeight = newBarHeight
        }
        .accessibilityIdentifier("main-tab-bar")
    }

    private var topBorder: some View {
        Rectangle()
            .fill(Color.cuppedCanvasBorder)
            .frame(height: 1)
    }

    private var barContent: some View {
        HStack(alignment: .bottom, spacing: 0) {
            ForEach(leftTabs, id: \.id) { tab in
                NavButton(
                    tab: tab,
                    isActive: selectedTab == tab.id,
                    namespace: tabNamespace,
                    onTap: { selectTab(tab.id) }
                )
            }

            CheckInButton(onTap: openLogSheet)

            ForEach(rightTabs, id: \.id) { tab in
                NavButton(
                    tab: tab,
                    isActive: selectedTab == tab.id,
                    namespace: tabNamespace,
                    onTap: { selectTab(tab.id) }
                )
            }
        }
        .padding(.horizontal, Spacing.base)
        .padding(.top, Spacing.sm)
        .padding(.bottom, Spacing.sm)
    }

    private var barBackground: some View {
        ZStack {
            Color.cuppedSurfaceCard.opacity(0.95)
            Rectangle().fill(.ultraThinMaterial)
        }
        .ignoresSafeArea(edges: .bottom)
    }

    private func selectTab(_ tab: MainTabId) {
        guard selectedTab != tab else { return }
        selectedTab = tab
    }

    private func openLogSheet() {
        presentedSheet = .log
    }
}

private struct NavButton: View {
    let tab: MainTabDisplaySpec
    let isActive: Bool
    let namespace: Namespace.ID
    let onTap: () -> Void

    private let pillWidth: CGFloat = 24
    private let pillHeight: CGFloat = 3
    private let pillToIconSpacing = Spacing.xs
    private let iconToLabelSpacing: CGFloat = 2

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 0) {
                Group {
                    if isActive {
                        Capsule()
                            .fill(Color.cuppedPrimary)
                            .frame(width: pillWidth, height: pillHeight)
                            .matchedGeometryEffect(id: "activePill", in: namespace)
                    } else {
                        Capsule()
                            .fill(Color.clear)
                            .frame(width: pillWidth, height: pillHeight)
                    }
                }
                .padding(.bottom, pillToIconSpacing)

                AppIconView(
                    icon: isActive ? tab.activeIcon : tab.inactiveIcon,
                    size: 22,
                    color: isActive ? Color.cuppedPrimary : Color.cuppedMuted
                )
                .scaleEffect(isActive ? 1.0 : 0.85)
                .offset(y: isActive ? -2 : 0)
                .padding(.bottom, iconToLabelSpacing)

                Text(tab.title)
                    .font(.cuppedCaption)
                    .foregroundStyle(isActive ? Color.cuppedPrimary : Color.cuppedMuted)
                    .opacity(isActive ? 1.0 : 0.5)
            }
            .frame(maxWidth: .infinity)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
        .accessibilityLabel(tab.title)
        .accessibilityAddTraits(isActive ? .isSelected : [])
        .accessibilityIdentifier("tab-\(tab.identifierSuffix)")
    }
}

private struct CheckInButton: View {
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            ZStack {
                Circle()
                    .fill(Color.cuppedPrimary)
                    .frame(width: 56, height: 56)

                Circle()
                    .strokeBorder(Color.cuppedCanvas, lineWidth: 4)
                    .frame(width: 56, height: 56)

                AppIconView(icon: .coffee, size: 28, color: Color.cuppedInkInverse)
            }
        }
        .buttonStyle(CheckInButtonStyle())
        .accessibilityLabel(Text("Log a Brew"))
        .accessibilityIdentifier("log-action-button")
        .modifier(Shadow.glowCoral)
        .offset(y: -Spacing.lg)
    }
}

private struct CheckInButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.92 : 1.0)
            .animation(.cuppedSpring, value: configuration.isPressed)
    }
}

#Preview {
    MainTabView()
}
