package cafe.cupped.app.viewmodel

/// Swift-visible value accessors for SmokeTestViewModel StateFlow properties.
/// KMP-ObservableViewModel handles observation internally, but SKIE wraps
/// StateFlow into SkieSwiftStateFlow which is incompatible with SwiftUI Text().
/// These extensions expose the unwrapped .value for direct use in Swift views.

val SmokeTestViewModel.greetingValue: String
    get() = greeting.value

val SmokeTestViewModel.isHealthyValue: Boolean
    get() = isHealthy.value

val SmokeTestViewModel.statusValue: String
    get() = status.value
