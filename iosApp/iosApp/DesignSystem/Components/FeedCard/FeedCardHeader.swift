import SwiftUI

struct FeedCardHeader: View {
    let user: FeedUser
    let postedAt: Date
    var onMore: (() -> Void)?

    var body: some View {
        HStack(alignment: .center, spacing: Spacing.sm) {
            // Avatar
            AsyncImage(url: user.avatarURL) { phase in
                switch phase {
                case .success(let image):
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                case .failure:
                    Image(systemName: "person.crop.circle.fill")
                        .resizable()
                        .foregroundStyle(Color.cuppedMuted)
                default:
                    SkeletonView(width: 40, height: 40, cornerRadius: 20)
                }
            }
            .frame(width: 40, height: 40)
            .clipShape(Circle())

            // Name + time
            VStack(alignment: .leading, spacing: 2) {
                Text(user.displayName)
                    .font(.cuppedText(size: 14, weight: .bold))
                    .foregroundStyle(Color.cuppedInk)

                Text(relativeTime)
                    .font(.cuppedCaption)
                    .foregroundStyle(Color.cuppedMuted)
            }

            Spacer()

            // More button
            if let onMore {
                Button(action: onMore) {
                    Image(systemName: "ellipsis")
                        .font(.system(size: 16, weight: .medium))
                        .foregroundStyle(Color.cuppedMuted)
                        .frame(width: 32, height: 32)
                        .contentShape(Rectangle())
                }
                .buttonStyle(TapScaleButtonStyle())
            }
        }
    }

    private var relativeTime: String {
        let formatter = RelativeDateTimeFormatter()
        formatter.unitsStyle = .abbreviated
        return formatter.localizedString(for: postedAt, relativeTo: Date())
    }
}

#Preview("Feed Card Header") {
    VStack(spacing: Spacing.lg) {
        FeedCardHeader(
            user: FeedUser(id: "1", displayName: "Sarah Chen", avatarURL: nil),
            postedAt: Date().addingTimeInterval(-7200),
            onMore: {}
        )

        FeedCardHeader(
            user: FeedUser(id: "2", displayName: "Alex Rivera", avatarURL: nil),
            postedAt: Date().addingTimeInterval(-86400)
        )
    }
    .padding()
    .background(Color.cuppedCanvas)
}
