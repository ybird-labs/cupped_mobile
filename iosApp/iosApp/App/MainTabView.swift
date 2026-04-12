import SwiftUI

private struct BarHeightKey: PreferenceKey {
    static var defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

enum Tab: String, CaseIterable {
    case feed, discover, community, profile

    var title: String {
        rawValue.capitalized
    }

    var icon: AppIcon {
        switch self {
        case .feed: .home
        case .discover: .discover
        case .community: .community
        case .profile: .profile
        }
    }

    var filledIcon: AppIcon {
        switch self {
        case .feed: .homeActive
        case .discover: .discoverActive
        case .community: .communityActive
        case .profile: .profileActive
        }
    }
}

struct MainTabView: View {
     @State private var selectedTab: Tab = .feed
     @State private var showLogSheet = false
     @State private var barHeight: CGFloat = 0
     @Namespace private var tabNamespace

     private var leftTabs: [Tab] { Array(Tab.allCases.prefix(2)) }
     private var rightTabs: [Tab] { Array(Tab.allCases.suffix(2)) }

     var body: some View {
         ZStack(alignment: .bottom) {
             tabContent(for: selectedTab)
                 .frame(maxWidth: .infinity, maxHeight: .infinity)
                 .padding(.bottom, barHeight)

             barView
         }
         .animation(.cuppedSpring, value: selectedTab)
         .sheet(isPresented: $showLogSheet) {
             LogView()
         }
     }

     // MARK: - Tab Content

     @ViewBuilder
     private func tabContent(for tab: Tab) -> some View {
         switch tab {
         case .feed:      FeedView()
         case .discover:  DiscoverView()
         case .community: CommunityView()
         case .profile:   ProfileView()
         }
     }

     // MARK: - Bar View

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
         .onPreferenceChange(BarHeightKey.self) { barHeight = $0 }
     }

     private var topBorder: some View {
         Rectangle()
             .fill(Color.cuppedCanvasBorder)
             .frame(height: 1)
     }

     private var barContent: some View {
         HStack(alignment: .bottom, spacing: 0) {
             ForEach(leftTabs, id: \.self) { tab in
                 NavButton(
                     tab: tab,
                     isActive: selectedTab == tab,
                     namespace: tabNamespace,
                     onTap: { selectedTab = tab }
                 )
             }

             CheckInButton(onTap: { showLogSheet = true })

             ForEach(rightTabs, id: \.self) { tab in
                 NavButton(
                     tab: tab,
                     isActive: selectedTab == tab,
                     namespace: tabNamespace,
                     onTap: { selectedTab = tab }
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
 }

private struct NavButton: View {
     let tab: Tab
     let isActive: Bool
     let namespace: Namespace.ID
     let onTap: () -> Void

     var body: some View {
         Button(action: onTap) {
             VStack(spacing: 2) {
                 // --- Pill indicator ---
                 // This is the small bar above the active icon.
                 // Only the active tab renders it. matchedGeometryEffect
                 // makes it animate its position from one tab to another
                 // because they all share the same id "activePill".
                 if isActive {
                     Capsule()
                         .fill(Color.cuppedPrimary)
                         .frame(width: 24, height: 3)
                         .matchedGeometryEffect(id: "activePill", in: namespace)
                 } else {
                     // Invisible placeholder so the layout doesn't jump
                     // when a tab becomes active. Same frame, just hidden.
                     Capsule()
                         .fill(Color.clear)
                         .frame(width: 24, height: 3)
                 }

                 // --- Icon ---
                 AppIconView(
                     icon: isActive ? tab.filledIcon : tab.icon,
                     size: 22,
                     color: isActive ? Color.cuppedPrimary : Color.cuppedMuted
                 )
                     .scaleEffect(isActive ? 1.0 : 0.85)
                     .offset(y: isActive ? -2 : 0)

                 // --- Label ---
                 Text(tab.title)
                     .font(.cuppedCaption)
                     .foregroundStyle(isActive ? Color.cuppedPrimary : Color.cuppedMuted)
                     .opacity(isActive ? 1.0 : 0.5)
             }
             .frame(maxWidth: .infinity)
             .contentShape(Rectangle()) // Makes the full area tappable
         }
         .buttonStyle(.plain) // Removes the default button highlight
     }
 }

private struct CheckInButton: View {
      let onTap: () -> Void
      @State private var isPressed = false

      var body: some View {
          Button(action: onTap) {
              ZStack {
                  // The circle
                  Circle()
                      .fill(Color.cuppedPrimary)
                      .frame(width: 56, height: 56)

                  // Border that makes it look "cut out" from the bar.
                  // cuppedCanvas matches the app background, so the
                  // border visually separates the button from the bar.
                  Circle()
                      .strokeBorder(Color.cuppedCanvas, lineWidth: 4)
                      .frame(width: 56, height: 56)

                  // Plus icon
                  AppIconView(icon: .add, size: 28, color: Color.cuppedInkInverse)
              }
          }
          .buttonStyle(CheckInButtonStyle())
          .modifier(Shadow.glowCoral)  // The coral glow from your shadow tokens
          // Shift it up so it "floats" above the bar edge
          .offset(y: -Spacing.lg)
          // The offset means the button takes up visual space above the bar
          // but the HStack still reserves its natural width. That's what we want.
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
