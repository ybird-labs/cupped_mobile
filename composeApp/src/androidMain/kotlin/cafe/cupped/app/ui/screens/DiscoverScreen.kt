package cafe.cupped.app.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import cafe.cupped.app.ui.theme.CuppedColor

@Composable
fun DiscoverScreen() {
    PlaceholderScreen(
        title = "Discover",
        subtitle = "Coming soon",
        icon = Icons.Filled.Search,
        contentTag = "screen-discover",
        iconTint = CuppedColor.ActionPrimary,
    )
}
