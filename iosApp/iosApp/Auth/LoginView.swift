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
    @State private var cardAppeared = false

    // MARK: - Computed

    /// The current UI state from the KMP AuthViewModel.
    private var currentState: AuthUiState {
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

    /// Whether the current state is an error.
    private var hasError: Bool {
        currentState is AuthUiStateError
    }

    /// Error message from the last failed attempt, if any.
    /// Sanitizes raw networking exceptions as a fallback
    /// (primary sanitization happens in KMP AuthViewModel).
    private var errorMessage: String? {
        guard let raw = (currentState as? AuthUiStateError)?.message else {
            return nil
        }
        return Self.sanitizeError(raw)
    }

    /// Belt-and-suspenders sanitization for any raw platform
    /// error messages that slip through the KMP layer.
    private static func sanitizeError(_ raw: String) -> String {
        let lowered = raw.lowercased()

        if lowered.contains("nsurlerrordomain")
            || lowered.contains("kcfstreamerrordomainkey")
            || lowered.contains("cfnetwork") {
            return "Unable to reach the server. Check your connection and try again."
        }

        if !raw.contains("{") && raw.count < 120 {
            return raw
        }

        return "Something went wrong. Please try again."
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
                        // Card entrance: fade + slide up (React: opacity 0→1, y 20→0)
                        .opacity(cardAppeared ? 1 : 0)
                        .offset(y: cardAppeared ? 0 : 20)

                    Spacer(minLength: Spacing.xxl)
                }
                .frame(maxWidth: .infinity)
            }
        }
        .onAuthenticatedCheck(state: currentState, action: onAuthenticated)
        .onAppear {
            withAnimation(
                .easeOut(duration: 0.5).delay(0.1)
            ) {
                cardAppeared = true
            }
        }
    }

    // MARK: - Card Content

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
            // Logo + title block with extra bottom margin (React: mb-8 = 32px)
            VStack(spacing: Spacing.base) {
                logoBlock
                titleBlock
                    .id(isRegisterMode)
            }
            .padding(.bottom, Spacing.sm) // extra gap: 20 (lg) + 8 (sm) ≈ 28pt

            emailFieldSection
            errorSection
            submitButton

            modeToggle
                .padding(.top, Spacing.sm)

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
                        .delay(0.3)
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
            .padding(.vertical, Spacing.base) // py-4 = 16px
            .background(Color.white)
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
                    hasError
                        ? Color.cuppedError
                        : Color.cuppedCanvasBorderSubtle,
                    lineWidth: 1
                )
            }
        }
    }

    // MARK: - Error Message
    // React spec: simple `text-sm text-error` — no icon, no background.

    @ViewBuilder
    private var errorSection: some View {
        if let error = errorMessage {
            Text(error)
                .font(.cuppedSubheadline)
                .foregroundStyle(Color.cuppedError)
                .frame(maxWidth: .infinity, alignment: .leading)
                .transition(
                    .opacity.combined(
                        with: .offset(y: -5)
                    )
                )
        }
    }

    // MARK: - Submit Button

    private var submitButton: some View {
        CuppedButton(
            title: isLoading
                ? "Sending magic link..."
                : (isRegisterMode ? "Create account" : "Sign in"),
            style: .primary,
            icon: isLoading ? nil : "arrow.right",
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
