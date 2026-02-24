package cafe.cupped.app.logging

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

object NapierInit {
    /** Call once from each platform's app startup */
    fun init() {
        Napier.base(DebugAntilog())
        Napier.d("Napier logging initialized")
    }
}
