import SwiftUI

struct CommunityView: View {
    var body: some View {
        NavigationStack {
            VStack(spacing: Spacing.lg) {
                Image(systemName: "person.2.fill")
                    .font(.system(size: 48))
                    .foregroundStyle(Color.cuppedPrimary)
                    .accessibilityHidden(true)

                Text("Community")
                    .font(.cuppedLargeTitle)
                    .foregroundStyle(Color.cuppedInk)

                Text("Coming soon")
                    .font(.cuppedBody)
                    .foregroundStyle(Color.cuppedMuted)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color.cuppedCanvas)
            .navigationTitle("Community")
        }
    }
}

#Preview {
    CommunityView()
}
