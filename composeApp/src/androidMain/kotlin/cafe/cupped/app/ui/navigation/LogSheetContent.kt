package cafe.cupped.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import cafe.cupped.app.ui.theme.CuppedColor
import cafe.cupped.app.ui.theme.CuppedSpacing
import cafe.cupped.composeapp.R

@Composable
fun LogSheetContent(
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = CuppedColor.SurfaceCard,
                shape = RoundedCornerShape(
                    topStart = CuppedSpacing.Xl,
                    topEnd = CuppedSpacing.Xl,
                ),
            )
            .padding(horizontal = CuppedSpacing.Xl, vertical = CuppedSpacing.Xl)
            .testTag("log-sheet"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CuppedSpacing.Sm),
    ) {
        Text(
            text = stringResource(R.string.log_action_button_label),
            color = CuppedColor.TextPrimary,
            fontSize = CuppedSpacing.SheetTitleTextSize,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.log_sheet_subtitle),
            color = CuppedColor.TextMuted,
            fontSize = CuppedSpacing.BodyTextSize,
        )
        TextButton(onClick = onDismiss) {
            Text(
                text = stringResource(R.string.log_sheet_dismiss),
                color = CuppedColor.ActionPrimary,
                fontSize = CuppedSpacing.BodyTextSize,
            )
        }
    }
}
