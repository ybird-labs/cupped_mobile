import SwiftUI
import Shared

// MARK: - LoginView

/// Native login/register screen — faithful SwiftUI translation of the
/// Magic Patterns AuthPage.tsx component.
///
/// Uses the KMP ``AuthViewModel`` (created via `KoinHelper.shared.makeAuthViewModel()`)
/// for state management. The view toggles inline between login and register modes
/// with cross-fade animations matching the React AnimatePresence pattern.
///
/// ## State Flow
/// - **Idle / Error** → Shows email input form
/// - **Loading** → Disables form, shows spinner on CTA
/// - **MagicLinkSent** → Switches to ``MagicLinkSentView``
/// - **Authenticated** → Calls ``onAuthenticated`` with bearer token
struct LoginView: View {

    /// Called with the bearer token when authentication succeeds.
    var onAuthenticated: (String) -> Void

    // MARK: - State

    @State private var authViewModel = KoinHelper.shared.makeAuthViewModel()
    @State private var email = ""
    @State private var isRegisterMode = false
    @State private var logoAppeared = false

    // MARK: - Computed

    /// The current UI state from the KMP AuthViewModel.
    private var currentState: AuthUiState {
        // KMP-ObservableViewModel makes AuthViewModel Observable.
        // Accessing uiState.value in the view body triggers SwiftUI
        // re-renders when the Kotlin StateFlow value changes.
        authViewModel.uiState.value as! AuthUiState
    }

    /// Basic client-side email validation (non-empty + contains "@").
    private var isEmailValid: Bool {
        let trimmed = email.trimmingCharacters(in: .whitespacesAndNewlines)
        return !trimmed.isEmpty && trimmed.contains("@")
    }

    /// Whether the form is currently submitting.
    private var isLoading: Bool {
        currentState is AuthUiStateLoading
    }

    /// Error message from the last failed attempt, if any.
    private var errorMessage: String? {
        (currentState as? AuthUiStateError)?.message
    }

    // MARK: - Body

    var body: some View {
        ZStack {
            Color.cuppedCanvas
                .ignoresSafeArea()

            ScrollView {
                VStack(spacing: 0) {
                    Spacer(minLength: Spacing.xxl)

                    cardContent
                        .padding(Spacing.xl)
                        .background(Color.cuppedCard)
                        .clipShape(
                            RoundedRectangle(
                                cornerRadius: Radius.xl,
                                style: .continuous
                            )
                        )
                        .overlay {
                            RoundedRectangle(
                                cornerRadius: Radius.xl,
                                style: .continuous
                            )
                            .strokeBorder(
                                Color.cuppedCanvasBorderSubtle,
                                lineWidth: 1
                            )
                        }
                        .modifier(Shadow.warmXl)
                        .frame(maxWidth: 400)
                        .padding(.horizontal, Spacing.lg)

                    Spacer(minLength: Spacing.xxl)
                }
                .frame(maxWidth: .infinity)
            }
        }
        .onAuthenticatedCheck(state: currentState, action: onAuthenticated)
    }

    // MARK: - Card Content

    /// The main card interior — switches between input form and success state.
    @ViewBuilder
    private var cardContent: some View {
        let state = currentState
        if let magicLinkState = state as? AuthUiStateMagicLinkSent {
            MagicLinkSentView(
                email: magicLinkState.email,
                isRegisterMode: isRegisterMode,
                onReset: {
                    authViewModel.reset()
                }
            )
            .transition(.scale.combined(with: .opacity))
        } else {
            inputForm
                .transition(.opacity)
        }
    }

    // MARK: - Input Form

    private var inputForm: some View {
        VStack(spacing: Spacing.lg) {
            logoBlock
            titleBlock
                .id(isRegisterMode)
            emailFieldSection
            errorSection
            submitButton
            modeToggle
            registerFeaturesSection
            footerText
        }
        .animation(.cuppedSpring, value: isRegisterMode)
    }

    // MARK: - Logo Block

    private var logoBlock: some View {
        RoundedRectangle(cornerRadius: Radius.lg, style: .continuous)
            .fill(Color.cuppedPrimary)
            .frame(width: 64, height: 64)
            .overlay {
                Image(systemName: "cup.and.saucer.fill")
                    .font(.system(size: 32))
                    .foregroundStyle(.white)
            }
            .modifier(Shadow.glowCoral)
            .scaleEffect(logoAppeared ? 1 : 0)
            .onAppear {
                withAnimation(
                    .spring(response: 0.5, dampingFraction: 0.65)
                ) {
                    logoAppeared = true
                }
            }
    }

    // MARK: - Title Block

    private var titleBlock: some View {
        VStack(spacing: Spacing.sm) {
            Text(isRegisterMode ? "Join Cupped" : "Welcome Back")
                .font(.cuppedTitle2)
                .fontWeight(.bold)
                .foregroundStyle(Color.cuppedInk)
                .multilineTextAlignment(.center)

            Text(
                isRegisterMode
                    ? "Start tracking your coffee journey today"
                    : "Sign in to continue your coffee journey"
            )
            .font(.cuppedBody)
            .foregroundStyle(Color.cuppedSecondary)
            .multilineTextAlignment(.center)
        }
    }

    // MARK: - Email Field

    private var emailFieldSection: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            Text("EMAIL ADDRESS")
                .font(.cuppedCaption)
                .fontWeight(.medium)
                .foregroundStyle(Color.cuppedSecondary)
                .tracking(1.2)

            HStack(spacing: Spacing.sm) {
                Image(systemName: "envelope")
                    .foregroundStyle(Color.cuppedMuted)
                    .font(.system(size: 16))

                TextField("your@email.com", text: $email)
                    .font(.cuppedBody)
                    .foregroundStyle(Color.cuppedInk)
                    .textContentType(.emailAddress)
                    .keyboardType(.emailAddress)
                    .autocorrectionDisabled()
                    .textInputAutocapitalization(.never)
            }
            .padding(.horizontal, Spacing.md)
            .padding(.vertical, Spacing.md)
            .background(Color.cuppedCanvas)
            .clipShape(
                RoundedRectangle(
                    cornerRadius: Radius.md,
                    style: .continuous
                )
            )
            .overlay {
                RoundedRectangle(
                    cornerRadius: Radius.md,
                    style: .continuous
                )
                .strokeBorder(
                    Color.cuppedCanvasBorderSubtle,
                    lineWidth: 1
                )
            }
        }
    }

    // MARK: - Error Message

    @ViewBuilder
    private var errorSection: some View {
        if let error = errorMessage {
            Text(error)
                .font(.cuppedCaption)
                .foregroundStyle(Color.cuppedError)
                .frame(maxWidth: .infinity, alignment: .leading)
                .transition(.opacity)
        }
    }

    // MARK: - Submit Button

    private var submitButton: some View {
        CuppedButton(
            title: isLoading
                ? "Sending magic link..."
                : (isRegisterMode ? "Create account" : "Sign in"),
            style: .primary,
            isLoading: isLoading,
            isDisabled: !isEmailValid
        ) {
            let trimmed = email.trimmingCharacters(
                in: .whitespacesAndNewlines
            )
            authViewModel.requestMagicLink(email: trimmed)
        }
    }

    // MARK: - Mode Toggle

    private var modeToggle: some View {
        HStack(spacing: Spacing.xs) {
            Text(
                isRegisterMode
                    ? "Already have an account?"
                    : "New to Cupped?"
            )
            .font(.cuppedSubheadline)
            .foregroundStyle(Color.cuppedSecondary)

            Button {
                withAnimation(.cuppedSpring) {
                    isRegisterMode.toggle()
                }
            } label: {
                Text(isRegisterMode ? "Sign in" : "Create account")
                    .font(.cuppedSubheadline)
                    .fontWeight(.medium)
                    .foregroundStyle(Color.cuppedPrimary)
            }
        }
    }

    // MARK: - Features Grid (Register Only)

    @ViewBuilder
    private var registerFeaturesSection: some View {
        if isRegisterMode {
            featuresGrid
                .transition(
                    .opacity.combined(with: .move(edge: .top))
                )
        }
    }

    private var featuresGrid: some View {
        VStack(spacing: Spacing.md) {
            Divider()

            Text("WHAT YOU'LL GET")
                .font(.cuppedCaption)
                .fontWeight(.medium)
                .foregroundStyle(Color.cuppedMuted)
                .tracking(1.2)

            HStack(spacing: Spacing.base) {
                FeatureItem(
                    icon: "cup.and.saucer",
                    label: "Track brews"
                )
                FeatureItem(icon: "sparkles", label: "Earn XP")
                FeatureItem(
                    icon: "safari",
                    label: "Discover beans"
                )
            }
        }
    }

    // MARK: - Footer

    private var footerText: some View {
        Text(
            "We'll send you a magic link to "
                + "\(isRegisterMode ? "get started" : "sign in")"
                + ". No password needed."
        )
        .font(.cuppedCaption)
        .foregroundStyle(Color.cuppedMuted)
        .multilineTextAlignment(.center)
    }
}

// MARK: - FeatureItem

/// A single feature icon + label for the register features grid.
private struct FeatureItem: View {
    let icon: String
    let label: String

    var body: some View {
        VStack(spacing: Spacing.sm) {
            Circle()
                .fill(Color.cuppedPrimaryLight)
                .frame(width: 40, height: 40)
                .overlay {
                    Image(systemName: icon)
                        .font(.system(size: 18))
                        .foregroundStyle(Color.cuppedPrimary)
                }

            Text(label)
                .font(.cuppedCaption)
                .foregroundStyle(Color.cuppedSecondary)
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - Authenticated State Check Modifier

/// View modifier that fires the onAuthenticated callback when
/// the AuthUiState transitions to `Authenticated`.
/// Uses `task(id:)` to avoid Equatable requirements on the
/// Kotlin protocol type.
private struct AuthenticatedCheckModifier: ViewModifier {
    let state: AuthUiState
    let action: (String) -> Void

    func body(content: Content) -> some View {
        content
            .task(id: ObjectIdentifier(state as AnyObject)) {
                if let authenticated = state as? AuthUiStateAuthenticated {
                    action(authenticated.bearerToken)
                }
            }
    }
}

extension View {
    fileprivate func onAuthenticatedCheck(
        state: AuthUiState,
        action: @escaping (String) -> Void
    ) -> some View {
        modifier(AuthenticatedCheckModifier(
            state: state,
            action: action
        ))
    }
}

// MARK: - Previews

#Preview("Login Mode") {
    LoginView { token in
        print("Authenticated with token: \(token)")
    }
}

#Preview("Register Mode") {
    LoginView { _ in }
}
