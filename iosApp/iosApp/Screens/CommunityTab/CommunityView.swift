import SwiftUI

struct CommunityView: View {
    var body: some View {
        NavigationStack {
            VStack(spacing: Spacing.lg) {
                AppIconView(icon: .communityActive, size: 48, color: Color.cuppedPrimary)

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
