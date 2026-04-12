package cafe.cupped.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import cafe.cupped.app.ui.navigation.MainShell
import cafe.cupped.app.ui.theme.CuppedColor

@Composable
fun App() {
    MaterialTheme(
        colorScheme = lightColorScheme(
            background = CuppedColor.SurfaceApp,
            surface = CuppedColor.SurfaceCard,
            primary = CuppedColor.ActionPrimary,
            onPrimary = CuppedColor.InkInverse,
            onBackground = CuppedColor.TextPrimary,
            onSurface = CuppedColor.TextPrimary,
        ),
    ) {
        MainShell()
    }
}
