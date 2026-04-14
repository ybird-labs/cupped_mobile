package cafe.cupped.app.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import cafe.cupped.app.ui.theme.CuppedColor
import cafe.cupped.composeapp.R

@Composable
fun CommunityScreen() {
    PlaceholderScreen(
        title = stringResource(R.string.tab_community),
        subtitle = stringResource(R.string.screen_community_subtitle),
        icon = Icons.Filled.Person,
        contentTag = "screen-community",
        iconTint = CuppedColor.ActionPrimary,
    )
}
