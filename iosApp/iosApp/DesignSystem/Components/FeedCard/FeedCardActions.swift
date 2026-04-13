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
                        AppIconView(
                            icon: .coffee,
                            size: 18,
                            color: isLiked ? Color.cuppedPrimary : Color.cuppedMuted
                        )
                        Text("\(likes)")
                            .font(.cuppedSubheadline)
                            .fontWeight(.medium)
                    }
                    .foregroundStyle(isLiked ? Color.cuppedPrimary : Color.cuppedMuted)
                }
                .buttonStyle(TapScaleButtonStyle())
                .disabled(onLike == nil)
                .opacity(onLike == nil ? 0.5 : 1)

                // Comment
                Button {
                    onComment?()
                } label: {
                    HStack(spacing: 6) {
                        AppIconView(icon: .comment, size: 18, color: Color.cuppedMuted)
                        Text("\(comments)")
                            .font(.cuppedSubheadline)
                            .fontWeight(.medium)
                    }
                    .foregroundStyle(Color.cuppedMuted)
                }
                .buttonStyle(TapScaleButtonStyle())
                .disabled(onComment == nil)
                .opacity(onComment == nil ? 0.5 : 1)

                Spacer()

                // Bookmark
                Button {
                    onBookmark?()
                } label: {
                    AppIconView(
                        icon: isBookmarked ? .bookmarkActive : .bookmark,
                        size: 18,
                        color: isBookmarked ? Color.cuppedXP : Color.cuppedMuted
                    )
                }
                .buttonStyle(TapScaleButtonStyle())
                .disabled(onBookmark == nil)
                .opacity(onBookmark == nil ? 0.5 : 1)
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
