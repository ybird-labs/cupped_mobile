package cafe.cupped.app.ui.theme

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.cupped.app.tokens.CuppedSpacing as SharedSpacing

object CuppedSpacing {
    val Xs = SharedSpacing.xs.dp
    val Sm = SharedSpacing.sm.dp
    val Base = SharedSpacing.base.dp
    val Lg = SharedSpacing.lg.dp
    val Xl = SharedSpacing.xl.dp

    val CaptionTextSize: TextUnit = 12.sp
    val BodyTextSize: TextUnit = 16.sp
    val SheetTitleTextSize: TextUnit = 24.sp
    val ScreenTitleTextSize: TextUnit = 32.sp
}
