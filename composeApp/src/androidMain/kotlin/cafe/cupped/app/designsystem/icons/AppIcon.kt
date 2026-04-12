package cafe.cupped.app.designsystem.icons

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import cafe.cupped.composeapp.R

enum class AppIcon(@DrawableRes val drawableRes: Int, val contentDescription: String) {
    Home(R.drawable.fa_home, "Home"),
    HomeActive(R.drawable.fa_home_active, "Home"),
    Discover(R.drawable.fa_discover, "Discover"),
    DiscoverActive(R.drawable.fa_discover_active, "Discover"),
    Community(R.drawable.fa_community, "Community"),
    CommunityActive(R.drawable.fa_community_active, "Community"),
    Profile(R.drawable.fa_profile, "Profile"),
    ProfileActive(R.drawable.fa_profile_active, "Profile"),
    Add(R.drawable.fa_add, "Add"),
    AddFilled(R.drawable.fa_add_filled, "Add"),
    Mail(R.drawable.fa_mail, "Email"),
    Forward(R.drawable.fa_forward, "Continue"),
    Coffee(R.drawable.fa_coffee, "Coffee"),
    Sparkles(R.drawable.fa_sparkles, "Sparkles"),
    Location(R.drawable.fa_location, "Location"),
    More(R.drawable.fa_more, "More"),
    Favorite(R.drawable.fa_favorite, "Favorite"),
    FavoriteActive(R.drawable.fa_favorite_active, "Favorite"),
    Comment(R.drawable.fa_comment, "Comment"),
    Bookmark(R.drawable.fa_bookmark, "Bookmark"),
    BookmarkActive(R.drawable.fa_bookmark_active, "Bookmark"),
    Box(R.drawable.fa_box, "Box"),
    User(R.drawable.fa_user, "User"),
    Rating(R.drawable.fa_rating, "Rating"),
    Success(R.drawable.fa_success, "Success"),
    Error(R.drawable.fa_error, "Error"),
    Warning(R.drawable.fa_warning, "Warning"),
    Info(R.drawable.fa_info, "Info"),
    Streak(R.drawable.fa_streak, "Streak"),
    Badge(R.drawable.fa_badge, "Badge"),
    Logout(R.drawable.fa_logout, "Sign out"),
}

@Composable
fun AppIcon(
    icon: AppIcon,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    contentDescription: String? = icon.contentDescription,
) {
    Image(
        painter = painterResource(icon.drawableRes),
        contentDescription = contentDescription,
        modifier = modifier,
        colorFilter = tint.takeIf { it != Color.Unspecified }?.let(ColorFilter::tint),
    )
}
