package cafe.cupped.app.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import cafe.cupped.app.tokens.CuppedMotion as SharedMotion

object CuppedMotion {
    val TabSpring = spring<Float>(
        dampingRatio = SharedMotion.springDamping.toFloat(),
        stiffness = Spring.StiffnessMediumLow,
    )
}
