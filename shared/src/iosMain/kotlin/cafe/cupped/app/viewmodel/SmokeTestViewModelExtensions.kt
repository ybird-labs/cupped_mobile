package cafe.cupped.app.viewmodel

// Swift-visible typed accessors for SmokeTestViewModel StateFlow properties.
// ObjC generics erasure causes StateFlow<String>.value to appear as Any? in Swift.
// These extensions expose typed values directly, per KMP-ObservableViewModel README.
// SKIE Flow interop is disabled for this package (see shared/build.gradle.kts)
// so KMP-ObservableViewModel's KVO/Observable observation works without interference.

val SmokeTestViewModel.greetingValue: String
    get() = greeting.value

val SmokeTestViewModel.isHealthyValue: Boolean
    get() = isHealthy.value

val SmokeTestViewModel.statusValue: String
    get() = status.value
