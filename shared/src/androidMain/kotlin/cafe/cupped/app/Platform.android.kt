package cafe.cupped.app

import android.os.Build
import cafe.cupped.app.shared.BuildConfig

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual val isDebug: Boolean = BuildConfig.DEBUG
