package cafe.cupped.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.cupped.app.ui.theme.CuppedColor
import cafe.cupped.app.ui.theme.CuppedSpacing

@Composable
internal fun PlaceholderScreen(
    title: String,
    subtitle: String,
    icon: ImageVector,
    contentTag: String,
    iconTint: Color,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CuppedColor.SurfaceApp)
            .padding(horizontal = CuppedSpacing.Xl)
            .testTag(contentTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier
                .padding(bottom = CuppedSpacing.Lg)
                .testTag("$contentTag-icon"),
        )
        Text(
            text = title,
            color = CuppedColor.TextPrimary,
            fontSize = CuppedSpacing.ScreenTitleTextSize,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = subtitle,
            color = CuppedColor.TextMuted,
            fontSize = CuppedSpacing.BodyTextSize,
            modifier = Modifier.padding(top = CuppedSpacing.Sm),
        )
    }
}
