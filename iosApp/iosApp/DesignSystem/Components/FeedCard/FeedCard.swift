import SwiftUI

struct FeedCard: View {
    let item: FeedItem
    var onLike: (() -> Void)?
    var onComment: (() -> Void)?
    var onBookmark: (() -> Void)?
    var onMore: (() -> Void)?
    var onBaristaTapped: ((String) -> Void)?

    var body: some View {
        CuppedCard {
            VStack(alignment: .leading, spacing: 0) {
                FeedCardHeader(
                    user: item.user,
                    postedAt: item.postedAt,
                    onMore: onMore
                )
                .padding(.bottom, Spacing.sm)

                FeedCardMedia(
                    photoURL: item.photoURL,
                    imageGradient: item.imageGradient,
                    flavors: item.flavors,
                    brewMethod: item.brewMethod,
                    venue: item.venue,
                    rating: item.rating
                )
                .padding(.horizontal, -Spacing.base)

                FeedCardContent(
                    coffee: item.coffee,
                    flavors: item.flavors,
                    notes: item.notes,
                    recipe: item.recipe,
                    onBaristaTapped: onBaristaTapped
                )
                .padding(.top, Spacing.base)

                FeedCardActions(
                    likes: item.likes,
                    comments: item.comments,
                    isLiked: item.isLiked,
                    isBookmarked: item.isBookmarked,
                    onLike: onLike,
                    onComment: onComment,
                    onBookmark: onBookmark
                )
                .padding(.top, Spacing.sm)
            }
        }
    }
}

// MARK: - Animated Variant

extension FeedCard {
    /// Wraps this card with a staggered entrance animation.
    func staggeredEntrance(index: Int) -> some View {
        modifier(StaggeredEntranceModifier(index: index))
    }
}

private struct StaggeredEntranceModifier: ViewModifier {
    let index: Int
    @State private var appeared = false

    func body(content: Content) -> some View {
        content
            .opacity(appeared ? 1 : 0)
            .offset(y: appeared ? 0 : 20)
            .animation(
                .cuppedSpring.delay(Double(index) * Motion.staggerDelay),
                value: appeared
            )
            .onAppear { appeared = true }
    }
}

// MARK: - Previews

#Preview("Feed Card - With Gradient") {
    ScrollView {
        FeedCard(item: .preview)
            .padding(.horizontal)
    }
    .background(Color.cuppedCanvas)
}

#Preview("Feed Card - No Photo") {
    ScrollView {
        FeedCard(item: .previewNoPhoto)
            .padding(.horizontal)
    }
    .background(Color.cuppedCanvas)
}

#Preview("Feed Card - Staggered") {
    ScrollView {
        VStack(spacing: Spacing.base) {
            FeedCard(item: .preview)
                .staggeredEntrance(index: 0)
            FeedCard(item: .previewNoPhoto)
                .staggeredEntrance(index: 1)
        }
        .padding(.horizontal)
    }
    .background(Color.cuppedCanvas)
}
