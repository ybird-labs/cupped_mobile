import SwiftUI

struct FeedCardMedia: View {
    let photoURL: URL?
    let imageGradient: [Color]?
    let flavors: [FlavorNote]
    let brewMethod: BrewMethod?
    let venue: VenueInfo?
    let rating: Double?

    /// Whether this shows a tall hero (4:3) or compact banner (5:2).
    private var hasHeroMedia: Bool {
        photoURL != nil
    }

    private var aspectRatio: CGFloat {
        hasHeroMedia ? 4.0 / 3.0 : 5.0 / 2.0
    }

    private var badgePadding: CGFloat {
        hasHeroMedia ? Spacing.base : Spacing.sm
    }

    var body: some View {
        ZStack(alignment: .topLeading) {
            // Background
            mediaBackground
                .aspectRatio(aspectRatio, contentMode: .fill)
                .clipped()

            // Badge overlays
            badgeOverlays
        }
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
                    SkeletonView(cornerRadius: 0)
                        .aspectRatio(aspectRatio, contentMode: .fill)
                }
            }
        } else if let imageGradient, !imageGradient.isEmpty {
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
                brewMethodBadge(brewMethod)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                    .padding(badgePadding)
            }

            // Top-right: Venue
            if let venue {
                venueBadge(venue)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)
                    .padding(badgePadding)
            }

            // Bottom-right: Rating
            if let rating {
                ratingBadge(rating)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomTrailing)
                    .padding(badgePadding)
            }
        }
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
        imageGradient: nil,
        flavors: [.chocolate, .nutty],
        brewMethod: .chemex,
        venue: VenueInfo(name: "Local Coffee", status: .claimed),
        rating: 8.5
    )
    .clipShape(RoundedRectangle(cornerRadius: Radius.md, style: .continuous))
    .padding()
}
