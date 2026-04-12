import SwiftUI

struct LogView: View {
    var body: some View {
        NavigationStack {
            VStack(spacing: Spacing.lg) {
                AppIconView(icon: .addFilled, size: 48, color: Color.cuppedPrimary)

                Text("Log a Brew")
                    .font(.cuppedLargeTitle)
                    .foregroundStyle(Color.cuppedInk)

                Text("Coming soon")
                    .font(.cuppedBody)
                    .foregroundStyle(Color.cuppedMuted)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color.cuppedCanvas)
            .navigationTitle("Log")
        }
    }
}

#Preview {
    LogView()
}
