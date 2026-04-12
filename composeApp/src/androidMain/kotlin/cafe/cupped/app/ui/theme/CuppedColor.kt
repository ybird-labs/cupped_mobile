package cafe.cupped.app.ui.theme

import androidx.compose.ui.graphics.Color
import cafe.cupped.app.tokens.ColorToken
import cafe.cupped.app.tokens.CuppedColors

object CuppedColor {
    val Canvas = CuppedColors.canvas.toComposeColor()
    val SurfaceCard = CuppedColors.surfaceCard.toComposeColor()
    val SurfaceApp = CuppedColors.surfaceApp.toComposeColor()
    val CanvasBorder = CuppedColors.canvasBorder.toComposeColor()
    val InkInverse = CuppedColors.inkInverse.toComposeColor()
    val TextPrimary = CuppedColors.textPrimary.toComposeColor()
    val TextMuted = CuppedColors.textMuted.toComposeColor()
    val Secondary = CuppedColors.secondary.toComposeColor()
    val Muted = CuppedColors.muted.toComposeColor()
    val ActionPrimary = CuppedColors.actionPrimary.toComposeColor()
}

private fun ColorToken.toComposeColor(): Color {
    val alpha = (alpha * 255f).toInt()
    val red = (red * 255f).toInt()
    val green = (green * 255f).toInt()
    val blue = (blue * 255f).toInt()
    val argbInt = android.graphics.Color.argb(
        alpha,
        red,
        green,
        blue,
    )
    return Color(argbInt)
}
