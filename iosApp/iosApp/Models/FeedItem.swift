import SwiftUI

struct FeedUser: Identifiable, Hashable {
    let id: String
    let displayName: String
    let avatarURL: URL?
}

struct CoffeeInfo: Hashable {
    let name: String
    let roaster: String?
    let barista: String?
    let farm: String?
    let origin: String?
}

struct VenueInfo: Hashable {
    let name: String
    let status: VenueStatus
}

struct RecipeInfo: Hashable {
    let ratio: String?
    let waterTemp: String?
    let grindSize: String?
}

struct FeedItem: Identifiable, Hashable {
    let id: String
    let user: FeedUser
    let postedAt: Date
    let coffee: CoffeeInfo
    let rating: Double?
    let brewMethod: BrewMethod?
    let venue: VenueInfo?
    let flavors: [FlavorNote]
    let notes: String?
    let recipe: RecipeInfo?
    let photoURL: URL?
    let imageGradient: [Color]?
    let likes: Int
    let comments: Int
    var isLiked: Bool
    var isBookmarked: Bool

    /// Whether the media area should use the tall (4:3) or compact (5:2) aspect ratio.
    var hasHeroMedia: Bool {
        photoURL != nil || imageGradient != nil
    }
}

// MARK: - Preview Data

extension FeedItem {
    static let preview = FeedItem(
        id: "1",
        user: FeedUser(
            id: "u1",
            displayName: "Sarah Chen",
            avatarURL: nil
        ),
        postedAt: Date().addingTimeInterval(-7200),
        coffee: CoffeeInfo(
            name: "Ethiopian Yirgacheffe",
            roaster: "Counter Culture",
            barista: "mike_barista",
            farm: "Konga Cooperative",
            origin: "Yirgacheffe, Ethiopia"
        ),
        rating: 9.2,
        brewMethod: .pourOver,
        venue: VenueInfo(name: "Blue Bottle, Hayes Valley", status: .curated),
        flavors: [.floral, .citrus],
        notes: "Absolutely stunning cup. The floral aromatics hit you immediately, followed by that classic tea-like body. @mike_barista nailed the extraction perfectly. Best I've had this year.",
        recipe: RecipeInfo(ratio: "1:16", waterTemp: "205\u{00B0}F", grindSize: "45"),
        photoURL: nil,
        imageGradient: [
            Color.cuppedPrimaryLight,
            Color.cuppedPrimary.opacity(0.6)
        ],
        likes: 24,
        comments: 5,
        isLiked: false,
        isBookmarked: true
    )

    static let previewNoPhoto = FeedItem(
        id: "2",
        user: FeedUser(
            id: "u2",
            displayName: "Alex Rivera",
            avatarURL: nil
        ),
        postedAt: Date().addingTimeInterval(-3600),
        coffee: CoffeeInfo(
            name: "Guatemala Huehuetenango",
            roaster: "Onyx Coffee Lab",
            barista: nil,
            farm: "Finca El Injerto",
            origin: "Huehuetenango, Guatemala"
        ),
        rating: 8.5,
        brewMethod: .chemex,
        venue: VenueInfo(name: "Local Coffee", status: .claimed),
        flavors: [.chocolate, .nutty, .sweet],
        notes: "Rich and chocolatey with a smooth finish.",
        recipe: RecipeInfo(ratio: "1:15", waterTemp: "200\u{00B0}F", grindSize: "55"),
        photoURL: nil,
        imageGradient: nil,
        likes: 12,
        comments: 3,
        isLiked: true,
        isBookmarked: false
    )
}
