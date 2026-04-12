package cafe.cupped.app.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.runtime.Composable
import cafe.cupped.app.ui.theme.CuppedColor

@Composable
fun ProfileScreen() {
    PlaceholderScreen(
        title = "Profile",
        subtitle = "Coming soon",
        icon = Icons.Filled.AccountCircle,
        contentTag = "screen-profile",
        iconTint = CuppedColor.ActionPrimary,
    )
}
