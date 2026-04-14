package cafe.cupped.app.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import cafe.cupped.app.ui.theme.CuppedColor
import cafe.cupped.composeapp.R

@Composable
fun FeedScreen() {
    PlaceholderScreen(
        title = stringResource(R.string.tab_feed),
        subtitle = stringResource(R.string.screen_feed_subtitle),
        icon = Icons.Filled.Home,
        contentTag = "screen-feed",
        iconTint = CuppedColor.ActionPrimary,
    )
}
