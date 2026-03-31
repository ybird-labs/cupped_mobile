import SwiftUI

struct DiscoverView: View {
    var body: some View {
        NavigationStack {
            VStack(spacing: Spacing.lg) {
                Image(systemName: "safari.fill")
                    .font(.system(size: 48))
                    .foregroundStyle(Color.cuppedPrimary)
                    .accessibilityHidden(true)

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
