import Foundation
import KMPObservableViewModelSwiftUI
import Shared
import SwiftUI

// MARK: - LoginView

/// Native login/register screen for magic-link auth.
///
/// This UI-only variant owns local form state and talks only to the shared
/// `AuthViewModel`. Session exchange, deep-link handling, and app-wide auth
/// coordination are intentionally left to a later plumbing-focused PR.
struct LoginView: View {

    /// Called with the bearer token when verification succeeds.
    var onAuthenticated: (String) -> Void

    @StateViewModel var authViewModel = KoinHelper.shared.makeAuthViewModel()
    @State private var email = ""
    @State private var isRegisterMode = true
    @State private var logoAppeared = false
    @State private var cardAppeared = false

    private var currentState: AuthUiState {
        authViewModel.uiStateValue
    }

    private var authenticatedToken: String? {
        (currentState as? AuthUiStateAuthenticated)?.bearerToken
    }

    private var isEmailValid: Bool {
        let trimmed = email.trimmingCharacters(in: .whitespacesAndNewlines)
        let pattern = #"^[^\s@]+@[^\s@]+\.[^\s@]+$"#
        let predicate = NSPredicate(format: "SELF MATCHES %@", pattern)
        return predicate.evaluate(with: trimmed)
    }

    private var isLoading: Bool {
        currentState is AuthUiStateLoading
    }

    private var errorMessage: String? {
        guard let raw = (currentState as? AuthUiStateError)?.message else {
            return nil
        }
        return Self.sanitizeError(raw)
    }

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

    var body: some View {
        GeometryReader { geometry in
            ScrollView {
                VStack(spacing: 0) {
                    cardContent
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
                        .opacity(cardAppeared ? 1 : 0)
                        .offset(y: cardAppeared ? 0 : 20)
                }
                .frame(maxWidth: .infinity)
                .frame(minHeight: geometry.size.height)
            }
            .scrollDismissesKeyboard(.interactively)
        }
        .background(Color.cuppedCanvas.ignoresSafeArea())
        .onChange(of: authenticatedToken) { _, token in
            if let token {
                onAuthenticated(token)
            }
        }
        .onAppear {
            withAnimation(
                .timingCurve(0.16, 1, 0.3, 1, duration: 0.5).delay(0.1)
            ) {
                cardAppeared = true
            }
        }
    }

    @ViewBuilder
    private var cardContent: some View {
        let state = currentState
        VStack(spacing: Spacing.lg) {
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
    }

    private var inputForm: some View {
        VStack(spacing: Spacing.lg) {
            VStack(spacing: Spacing.base) {
                logoBlock
                titleBlock
                    .id(isRegisterMode)
            }
            .padding(.bottom, Spacing.md)

            emailFieldSection
            submitButton

            modeToggle
                .padding(.top, Spacing.xs)

            registerFeaturesSection

            footerText
                .padding(.top, Spacing.md)
        }
        .animation(.cuppedSpring, value: isRegisterMode)
    }

    private var logoBlock: some View {
        RoundedRectangle(cornerRadius: Radius.lg, style: .continuous)
            .fill(Color.cuppedPrimary)
            .frame(width: 64, height: 64)
            .overlay {
                Image(systemName: "cup.and.saucer.fill")
                    .font(.system(size: 32))
                    .foregroundStyle(.white)
            }
            .shadow(
                color: Color.cuppedPrimary.opacity(0.3),
                radius: 10,
                x: 0,
                y: 4
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
                    authViewModel.reset()
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
        .padding(.top, Spacing.md)
    }

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

#Preview("Login Mode") {
    LoginView { token in
        print("Authenticated with token: \(token)")
    }
}

#Preview("Register Mode") {
    LoginView { _ in }
}
