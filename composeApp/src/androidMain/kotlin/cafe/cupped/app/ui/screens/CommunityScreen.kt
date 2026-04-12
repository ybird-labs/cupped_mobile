package cafe.cupped.app.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import cafe.cupped.app.ui.theme.CuppedColor

@Composable
fun CommunityScreen() {
    PlaceholderScreen(
        title = "Community",
        subtitle = "Coming soon",
        icon = Icons.Filled.Person,
        contentTag = "screen-community",
        iconTint = CuppedColor.ActionPrimary,
    )
}
