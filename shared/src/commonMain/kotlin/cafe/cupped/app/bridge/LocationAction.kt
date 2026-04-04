package cafe.cupped.app.bridge

import kotlinx.serialization.Serializable

@Serializable
enum class LocationAccuracy {
    HIGH,
    BALANCED,
    LOW
}
