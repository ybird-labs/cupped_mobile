import SwiftUI

struct FeedCardMedia: View {
    let photoURL: URL?
    let imageGradient: [Color]
    let flavors: [FlavorNote]
    let brewMethod: BrewMethod?
    let venue: VenueInfo?
    let rating: Double?

    /// Whether this shows a tall hero (4:3) or compact banner (5:2).
    private var hasHeroMedia: Bool {
        photoURL != nil || !imageGradient.isEmpty
    }

    private var aspectRatio: CGFloat {
        hasHeroMedia ? 4.0 / 3.0 : 5.0 / 2.0
    }

    private var badgePadding: CGFloat {
        hasHeroMedia ? Spacing.base : Spacing.sm
    }

    var body: some View {
        Color.clear
            .aspectRatio(aspectRatio, contentMode: .fit)
            .background { mediaBackground }
            .clipped()
            .overlay { badgeOverlays }
    }

    // MARK: - Background

    @ViewBuilder
    private var mediaBackground: some View {
        if let photoURL {
            AsyncImage(url: photoURL) { phase in
                switch phase {
                case .success(let image):
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                case .failure:
                    fallbackGradient
                default:
                    Color.cuppedCanvasBorder
                }
            }
        } else if !imageGradient.isEmpty {
            LinearGradient(
                colors: imageGradient,
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        } else {
            flavorGradient
        }
    }

    private var flavorGradient: some View {
        let colors: [Color] = {
            guard !flavors.isEmpty else {
                return [.cuppedPrimaryLight, Color.cuppedPrimary.opacity(0.6)]
            }
            if flavors.count == 1 {
                return [flavors[0].color.opacity(0.3), flavors[0].color.opacity(0.6)]
            }
            return Array(flavors.prefix(3).map { $0.color.opacity(0.4) })
        }()

        return LinearGradient(
            colors: colors,
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }

    private var fallbackGradient: some View {
        LinearGradient(
            colors: [.cuppedPrimaryLight, Color.cuppedPrimary.opacity(0.6)],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }

    // MARK: - Badge Overlays

    private var badgeOverlays: some View {
        ZStack {
            // Top-left: Brew method
            if let brewMethod {
                positioned(brewMethodBadge(brewMethod), alignment: .topLeading)
            }

            // Top-right: Venue
            if let venue {
                positioned(venueBadge(venue), alignment: .topTrailing)
            }

            // Bottom-right: Rating
            if let rating, isValidRating(rating) {
                positioned(ratingBadge(rating), alignment: .bottomTrailing)
            }
        }
    }

    private func positioned<C: View>(_ content: C, alignment: Alignment) -> some View {
        content
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: alignment)
            .padding(badgePadding)
    }

    private func brewMethodBadge(_ method: BrewMethod) -> some View {
        HStack(spacing: Spacing.xs) {
            Image(systemName: method.icon)
                .font(.system(size: 12, weight: .semibold))
            Text(method.label)
                .font(.cuppedCaption)
                .fontWeight(.bold)
                .tracking(0.3)
        }
        .foregroundStyle(.white)
        .padding(.horizontal, Spacing.sm)
        .padding(.vertical, 6)
        .background(Color.black.opacity(0.7))
        .clipShape(Capsule())
    }

    private func venueBadge(_ venue: VenueInfo) -> some View {
        HStack(spacing: Spacing.xs) {
            Image(systemName: "mappin")
                .font(.system(size: 10, weight: .semibold))
            Text(venue.name)
                .font(.cuppedCaption)
                .fontWeight(.medium)
        }
        .foregroundStyle(venue.status.foregroundColor)
        .padding(.horizontal, Spacing.sm)
        .padding(.vertical, 6)
        .background(venue.status.backgroundColor)
        .overlay(
            Capsule()
                .strokeBorder(venue.status.borderColor, lineWidth: 1)
        )
        .clipShape(Capsule())
    }

    private func ratingBadge(_ rating: Double) -> some View {
        HStack(spacing: Spacing.xs) {
            Image(systemName: "star.fill")
                .font(.system(size: 10))
                .foregroundStyle(Color.cuppedXP)
            Text(String(format: "%.1f/10", rating))
                .font(.cuppedCaption)
                .fontWeight(.bold)
                .foregroundStyle(Color.cuppedInk)
        }
        .padding(.horizontal, Spacing.sm)
        .padding(.vertical, 6)
        .background(Color.white.opacity(0.95))
        .overlay(
            Capsule()
                .strokeBorder(Color.cuppedCanvasBorder.opacity(0.5), lineWidth: 1)
        )
        .clipShape(Capsule())
    }

    private func isValidRating(_ rating: Double) -> Bool {
        rating.isFinite && rating >= 0 && rating <= 10
    }
}

#Preview("Media - With Gradient") {
    FeedCardMedia(
        photoURL: nil,
        imageGradient: [.cuppedPrimaryLight, Color.cuppedPrimary.opacity(0.6)],
        flavors: [.floral, .citrus],
        brewMethod: .pourOver,
        venue: VenueInfo(name: "Blue Bottle, Hayes Valley", status: .curated),
        rating: 9.2
    )
    .clipShape(RoundedRectangle(cornerRadius: Radius.md, style: .continuous))
    .padding()
}

#Preview("Media - Compact (No Photo)") {
    FeedCardMedia(
        photoURL: nil,
        imageGradient: [],
        flavors: [.chocolate, .nutty],
        brewMethod: .chemex,
        venue: VenueInfo(name: "Local Coffee", status: .claimed),
        rating: 8.5
    )
    .clipShape(RoundedRectangle(cornerRadius: Radius.md, style: .continuous))
    .padding()
}
