import SwiftUI

struct FeedCardActions: View {
    let likes: Int
    let comments: Int
    let isLiked: Bool
    let isBookmarked: Bool
    var onLike: (() -> Void)?
    var onComment: (() -> Void)?
    var onBookmark: (() -> Void)?

    var body: some View {
        VStack(spacing: 0) {
            Divider()

            HStack(spacing: Spacing.base) {
                // Like
                Button {
                    onLike?()
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: isLiked ? "heart.fill" : "heart")
                            .font(.system(size: 18))
                        Text("\(likes)")
                            .font(.cuppedSubheadline)
                            .fontWeight(.medium)
                    }
                    .foregroundStyle(isLiked ? Color.cuppedPrimary : Color.cuppedMuted)
                }
                .buttonStyle(TapScaleButtonStyle())

                // Comment
                Button {
                    onComment?()
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: "bubble.right")
                            .font(.system(size: 18))
                        Text("\(comments)")
                            .font(.cuppedSubheadline)
                            .fontWeight(.medium)
                    }
                    .foregroundStyle(Color.cuppedMuted)
                }
                .buttonStyle(TapScaleButtonStyle())

                Spacer()

                // Bookmark
                Button {
                    onBookmark?()
                } label: {
                    Image(systemName: isBookmarked ? "bookmark.fill" : "bookmark")
                        .font(.system(size: 18))
                        .foregroundStyle(isBookmarked ? Color.cuppedXP : Color.cuppedMuted)
                }
                .buttonStyle(TapScaleButtonStyle())
            }
            .padding(.top, Spacing.sm)
        }
    }
}

#Preview("Feed Card Actions") {
    VStack(spacing: Spacing.xl) {
        FeedCardActions(
            likes: 24,
            comments: 5,
            isLiked: false,
            isBookmarked: true
        )

        FeedCardActions(
            likes: 42,
            comments: 12,
            isLiked: true,
            isBookmarked: false
        )
    }
    .padding()
    .background(Color.cuppedCard)
}
