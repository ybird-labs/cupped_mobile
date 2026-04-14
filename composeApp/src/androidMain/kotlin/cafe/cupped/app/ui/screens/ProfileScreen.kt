package cafe.cupped.app.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import cafe.cupped.app.ui.theme.CuppedColor
import cafe.cupped.composeapp.R

@Composable
fun ProfileScreen() {
    PlaceholderScreen(
        title = stringResource(R.string.tab_profile),
        subtitle = stringResource(R.string.screen_profile_subtitle),
        icon = Icons.Filled.AccountCircle,
        contentTag = "screen-profile",
        iconTint = CuppedColor.ActionPrimary,
    )
}
