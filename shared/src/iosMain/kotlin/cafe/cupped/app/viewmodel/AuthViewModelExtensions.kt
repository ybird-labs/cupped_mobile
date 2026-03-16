package cafe.cupped.app.viewmodel

val AuthViewModel.uiStateValue: AuthUiState
    get() = uiState.value
