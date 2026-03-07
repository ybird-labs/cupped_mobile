import KMPObservableViewModelSwiftUI
import SwiftUI
import Shared
import Foundation

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

    @Environment(AuthCoordinator.self) private var authCoordinator

    // MARK: - State

    @StateViewModel var authViewModel = KoinHelper.shared.makeAuthViewModel()
    @State private var email = ""
    @State private var isRegisterMode = true
    @State private var logoAppeared = false
    @State private var cardAppeared = false


    // MARK: - Computed

    /// The current UI state from the KMP AuthViewModel.
    private var currentState: AuthUiState {
        authViewModel.uiStateValue
    }

    /// Non-nil only when the KMP AuthViewModel reaches
    /// the Authenticated state. Used as the `.onChange`
    /// trigger for the `onAuthenticated` callback.
    /// `String?` is `Equatable`, so `.onChange(of:)` works
    /// natively without any custom identity logic.
    private var authenticatedToken: String? {
        (currentState as? AuthUiStateAuthenticated)?
            .bearerToken
    }

    /// Lightweight client-side email validation used only to gate the CTA.
    private var isEmailValid: Bool {
        let trimmed = email.trimmingCharacters(in: .whitespacesAndNewlines)
        let pattern = #"^[^\s@]+@[^\s@]+\.[^\s@]+$"#
        let predicate = NSPredicate(format: "SELF MATCHES %@", pattern)
        return predicate.evaluate(with: trimmed)
    }

    /// Whether the form is currently submitting.
    private var isLoading: Bool {
        currentState is AuthUiStateLoading
    }

    private var isProcessingMagicLink: Bool {
        authCoordinator.authFlowStatus == .verifyingMagicLink
            || authCoordinator.authFlowStatus == .establishingSession
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
        GeometryReader { geometry in
            ScrollView {
                VStack(spacing: 0) {
                    cardContent
                        // React: p-8 md:p-10 → 32pt (match p-8)
                        .padding(Spacing.xxl)
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
                                Color.cuppedCanvasBorder,
                                lineWidth: 1
                            )
                        }
                        .modifier(Shadow.warmXl)
                        .frame(maxWidth: 400)
                        .padding(.horizontal, Spacing.lg)
                        // Card entrance: fade + slide up
                        // React: ease [0.16, 1, 0.3, 1], duration 0.5s
                        .opacity(cardAppeared ? 1 : 0)
                        .offset(y: cardAppeared ? 0 : 20)
                }
                .frame(maxWidth: .infinity)
                // Ensure content is at least screen height so VStack centers
                .frame(minHeight: geometry.size.height)
            }
            .scrollDismissesKeyboard(.interactively)
        }
        .background(Color.cuppedCanvas.ignoresSafeArea())
        .onChange(of: authenticatedToken) { _, token in
            if let token { onAuthenticated(token) }
        }
        .onAppear {
            withAnimation(
                .timingCurve(0.16, 1, 0.3, 1, duration: 0.5).delay(0.1)
            ) {
                cardAppeared = true
            }
        }
    }

    // MARK: - Card Content

    @ViewBuilder
    private var cardContent: some View {
        let state = currentState
        VStack(spacing: Spacing.lg) {
            if let magicLinkState = state as? AuthUiStateMagicLinkSent {
                MagicLinkSentView(
                    email: magicLinkState.email,
                    isRegisterMode: isRegisterMode,
                    onReset: {
                        authCoordinator.clearAuthFlowStatus()
                        authViewModel.reset()
                    }
                )
                .transition(.scale.combined(with: .opacity))
            } else {
                inputForm
                    .transition(.opacity)
            }
        }
    }

    // MARK: - Input Form

    private var inputForm: some View {
        VStack(spacing: Spacing.lg) {
            // Logo + title block
            // React: mb-8 (32px) from branding to form
            VStack(spacing: Spacing.base) {
                logoBlock
                titleBlock
                    .id(isRegisterMode)
            }
            .padding(.bottom, Spacing.md) // extra gap: 20 (lg) + 12 (md) = 32pt ≈ React mb-8

            emailFieldSection
            submitButton

            // React: mt-6 (24px)
            modeToggle
                .padding(.top, Spacing.xs)

            registerFeaturesSection

            // React: mt-8 (32px) for footer
            footerText
                .padding(.top, Spacing.md)
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
            // React: shadow-lg shadow-primary/30
            .shadow(
                color: Color.cuppedPrimary.opacity(0.3),
                radius: 10, x: 0, y: 4
            )
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
        CuppedTextField(
            placeholder: "your@email.com",
            text: $email,
            label: "EMAIL ADDRESS",
            icon: "envelope",
            error: errorMessage,
            isLoading: isLoading,
            keyboardType: .emailAddress,
            textContentType: .emailAddress,
            autocapitalization: .never,
            disableAutocorrection: true
        )
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
            isDisabled: !isEmailValid || isProcessingMagicLink
        ) {
            let trimmed = email.trimmingCharacters(
                in: .whitespacesAndNewlines
            )
            authCoordinator.clearAuthFlowStatus()
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
                    authCoordinator.clearAuthFlowStatus()
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

            // React: text-xs uppercase tracking-wider
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
        // React: mt-8 pt-8 border-t
        .padding(.top, Spacing.md)
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



// MARK: - Previews

#Preview("Login Mode") {
    LoginView { token in
        print("Authenticated with token: \(token)")
    }
    .environment(AuthCoordinator())
}

#Preview("Register Mode") {
    LoginView { _ in }
        .environment(AuthCoordinator())
}
