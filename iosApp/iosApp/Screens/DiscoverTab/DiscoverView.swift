import SwiftUI

struct DiscoverView: View {
    var body: some View {
        NavigationStack {
            VStack(spacing: Spacing.lg) {
                AppIconView(icon: .discoverActive, size: 48, color: Color.cuppedPrimary)

                Text("Discover")
                    .font(.cuppedLargeTitle)
                    .foregroundStyle(Color.cuppedInk)

                Text("Coming soon")
                    .font(.cuppedBody)
                    .foregroundStyle(Color.cuppedMuted)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color.cuppedCanvas)
            .navigationTitle("Discover")
        }
    }
}

#Preview {
    DiscoverView()
}
