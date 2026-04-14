package cafe.cupped.app.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import cafe.cupped.app.ui.theme.CuppedColor
import cafe.cupped.composeapp.R

@Composable
fun DiscoverScreen() {
    PlaceholderScreen(
        title = stringResource(R.string.tab_discover),
        subtitle = stringResource(R.string.screen_discover_subtitle),
        icon = Icons.Filled.Search,
        contentTag = "screen-discover",
        iconTint = CuppedColor.ActionPrimary,
    )
}
