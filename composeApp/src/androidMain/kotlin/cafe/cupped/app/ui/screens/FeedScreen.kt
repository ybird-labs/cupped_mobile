package cafe.cupped.app.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import cafe.cupped.app.ui.theme.CuppedColor

@Composable
fun FeedScreen() {
    PlaceholderScreen(
        title = "Feed",
        subtitle = "Android shell placeholder",
        icon = Icons.Filled.Home,
        contentTag = "screen-feed",
        iconTint = CuppedColor.ActionPrimary,
    )
}
