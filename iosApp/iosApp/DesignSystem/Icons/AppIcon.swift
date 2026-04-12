import SwiftUI

enum AppIcon: String, CaseIterable {
    case home
    case homeActive
    case discover
    case discoverActive
    case community
    case communityActive
    case profile
    case profileActive
    case add
    case addFilled
    case mail
    case forward
    case coffee
    case sparkles
    case location
    case more
    case favorite
    case favoriteActive
    case comment
    case bookmark
    case bookmarkActive
    case box
    case user
    case rating
    case success
    case error
    case warning
    case info
    case streak
    case badge
    case logout

    var assetName: String {
        switch self {
        case .home: "fa_home"
        case .homeActive: "fa_home_active"
        case .discover: "fa_discover"
        case .discoverActive: "fa_discover_active"
        case .community: "fa_community"
        case .communityActive: "fa_community_active"
        case .profile: "fa_profile"
        case .profileActive: "fa_profile_active"
        case .add: "fa_add"
        case .addFilled: "fa_add_filled"
        case .mail: "fa_mail"
        case .forward: "fa_forward"
        case .coffee: "fa_coffee"
        case .sparkles: "fa_sparkles"
        case .location: "fa_location"
        case .more: "fa_more"
        case .favorite: "fa_favorite"
        case .favoriteActive: "fa_favorite_active"
        case .comment: "fa_comment"
        case .bookmark: "fa_bookmark"
        case .bookmarkActive: "fa_bookmark_active"
        case .box: "fa_box"
        case .user: "fa_user"
        case .rating: "fa_rating"
        case .success: "fa_success"
        case .error: "fa_error"
        case .warning: "fa_warning"
        case .info: "fa_info"
        case .streak: "fa_streak"
        case .badge: "fa_badge"
        case .logout: "fa_logout"
        }
    }

    var accessibilityLabel: String {
        switch self {
        case .home, .homeActive: "Home"
        case .discover, .discoverActive: "Discover"
        case .community, .communityActive: "Community"
        case .profile, .profileActive: "Profile"
        case .add, .addFilled: "Add"
        case .mail: "Email"
        case .forward: "Continue"
        case .coffee: "Coffee"
        case .sparkles: "Sparkles"
        case .location: "Location"
        case .more: "More"
        case .favorite, .favoriteActive: "Favorite"
        case .comment: "Comment"
        case .bookmark, .bookmarkActive: "Bookmark"
        case .box: "Box"
        case .user: "User"
        case .rating: "Rating"
        case .success: "Success"
        case .error: "Error"
        case .warning: "Warning"
        case .info: "Info"
        case .streak: "Streak"
        case .badge: "Badge"
        case .logout: "Sign out"
        }
    }
}

struct AppIconView: View {
    let icon: AppIcon
    var size: CGFloat = 20
    var color: Color? = nil
    var decorative: Bool = true

    var body: some View {
        let image = Image(icon.assetName)
            .renderingMode(.template)
            .resizable()
            .scaledToFit()
            .frame(width: size, height: size)

        if decorative {
            image
                .foregroundStyle(color ?? .primary)
                .accessibilityHidden(true)
        } else {
            image
                .foregroundStyle(color ?? .primary)
                .accessibilityLabel(icon.accessibilityLabel)
        }
    }
}
