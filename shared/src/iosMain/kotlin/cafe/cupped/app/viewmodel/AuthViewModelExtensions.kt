package cafe.cupped.app.viewmodel

// Swift-visible typed accessor for AuthViewModel StateFlow property.
// ObjC generics erasure causes StateFlow<AuthUiState>.value to appear as Any? in Swift.
// This extension exposes a typed value directly, per KMP-ObservableViewModel README.
// SKIE Flow interop is disabled for this package (see shared/build.gradle.kts)
// so KMP-ObservableViewModel's KVO/Observable observation works without interference.

val AuthViewModel.uiStateValue: AuthUiState
    get() = uiState.value
