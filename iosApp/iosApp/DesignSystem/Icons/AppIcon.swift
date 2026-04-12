import SwiftUI
import UIKit

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
    case coffeePlus
    case coffeeAeropress
    case coffeeAeropressFilled
    case coffeeChemex
    case coffeeChemexFilled
    case coffeePot
    case coffeePotFilled
    case coffeeTogo
    case coffeeTogoFilled
    case coffeeBean
    case coffeeBeanFilled
    case coffeeBeans
    case coffeeBeansFilled
    case sparkles
    case location
    case ratio
    case temperature
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
    case store
    case storeFilled
    case storePlus
    case storePlusFilled
    case coffeeBeanPlus
    case coffeeBeanPlusFilled
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
        case .coffeePlus: "fa_coffee_plus"
        case .coffeeAeropress: "fa_coffee_aeropress"
        case .coffeeAeropressFilled: "fa_coffee_aeropress_filled"
        case .coffeeChemex: "fa_coffee_chemex"
        case .coffeeChemexFilled: "fa_coffee_chemex_filled"
        case .coffeePot: "fa_coffee_pot"
        case .coffeePotFilled: "fa_coffee_pot_filled"
        case .coffeeTogo: "fa_coffee_togo"
        case .coffeeTogoFilled: "fa_coffee_togo_filled"
        case .coffeeBean: "fa_coffee_bean"
        case .coffeeBeanFilled: "fa_coffee_bean_filled"
        case .coffeeBeans: "fa_coffee_beans"
        case .coffeeBeansFilled: "fa_coffee_beans_filled"
        case .sparkles: "fa_sparkles"
        case .location: "fa_location"
        case .ratio: "fa_ratio"
        case .temperature: "fa_temperature"
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
        case .store: "fa_store"
        case .storeFilled: "fa_store_filled"
        case .storePlus: "fa_store_plus"
        case .storePlusFilled: "fa_store_plus_filled"
        case .coffeeBeanPlus: "fa_coffee_bean_plus"
        case .coffeeBeanPlusFilled: "fa_coffee_bean_plus_filled"
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
        case .coffeePlus: "Add coffee"
        case .coffeeAeropress, .coffeeAeropressFilled: "Aeropress"
        case .coffeeChemex, .coffeeChemexFilled: "Chemex"
        case .coffeePot, .coffeePotFilled: "Coffee pot"
        case .coffeeTogo, .coffeeTogoFilled: "Coffee to go"
        case .coffeeBean, .coffeeBeanFilled: "Coffee bean"
        case .coffeeBeans, .coffeeBeansFilled: "Coffee beans"
        case .sparkles: "Sparkles"
        case .location: "Location"
        case .ratio: "Ratio"
        case .temperature: "Temperature"
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
        case .store, .storeFilled: "Store"
        case .storePlus, .storePlusFilled: "Add store"
        case .coffeeBeanPlus, .coffeeBeanPlusFilled: "Add coffee bean"
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
        let assetName = icon.assetName
        #if DEBUG
        let _ = {
            if ProcessInfo.processInfo.environment["XCODE_RUNNING_FOR_PREVIEWS"] != "1" {
                assert(UIImage(named: assetName) != nil, "Missing Font Awesome asset: \(assetName)")
            }
            return true
        }()
        #endif
        let image = Image(assetName)
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
