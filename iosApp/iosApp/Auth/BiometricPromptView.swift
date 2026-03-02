// BiometricPromptView.swift
// Cupped - cafe.cupped.app
//
// Post-login opt-in prompt for Face ID / Touch ID.
// Shown once after first successful login when biometrics
// are available but not yet enabled. The user can enable
// biometrics for faster sign-in or skip. This is a one-time
// prompt — the "prompted" flag in UserDefaults prevents
// re-display.

import SwiftUI

/// Full-screen biometric opt-in prompt matching the Cupped
/// design system card-on-canvas pattern.
///
/// ## Usage
/// ```swift
/// BiometricPromptView {
///     // Navigate away — user chose or skipped
/// }
/// ```
struct BiometricPromptView: View {

    /// Called when the user enables biometrics or skips.
    let onComplete: () -> Void

    // MARK: - State

    /// Controls the scale-in spring animation on the icon.
    @State private var iconAppeared = false

    // MARK: - Computed

    /// Human-readable biometric name ("Face ID" / "Touch ID").
    private var biometricName: String {
        BiometricService.shared.biometricName
    }

    /// SF Symbol name for the current biometric type.
    private var biometricIcon: String {
        switch BiometricService.shared.availableType {
        case .faceID: "faceid"
        case .touchID: "touchid"
        case .none: "lock.shield"
        }
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
    }

    // MARK: - Card Content

    private var cardContent: some View {
        VStack(spacing: Spacing.lg) {
            biometricIconView
            titleSection
            descriptionSection
            enableButton
            skipButton
            footerSection
        }
    }

    // MARK: - Biometric Icon

    private var biometricIconView: some View {
        Circle()
            .fill(Color.cuppedPrimary.opacity(0.1))
            .frame(width: 64, height: 64)
            .overlay {
                Image(systemName: biometricIcon)
                    .font(.system(size: 32))
                    .foregroundStyle(Color.cuppedPrimary)
            }
            .scaleEffect(iconAppeared ? 1 : 0)
            .onAppear {
                withAnimation(
                    .spring(response: 0.5, dampingFraction: 0.65)
                ) {
                    iconAppeared = true
                }
            }
    }

    // MARK: - Title

    private var titleSection: some View {
        Text("Enable \(biometricName)")
            .font(.cuppedTitle2)
            .fontWeight(.bold)
            .foregroundStyle(Color.cuppedInk)
            .multilineTextAlignment(.center)
    }

    // MARK: - Description

    private var descriptionSection: some View {
        Text(
            "Sign in faster next time with \(biometricName). "
                + "Your account stays secure."
        )
        .font(.cuppedBody)
        .foregroundStyle(Color.cuppedSecondary)
        .multilineTextAlignment(.center)
    }

    // MARK: - Enable Button

    private var enableButton: some View {
        CuppedButton(
            title: "Enable \(biometricName)",
            style: .primary
        ) {
            BiometricService.shared.isEnabled = true
            onComplete()
        }
    }

    // MARK: - Skip Button

    private var skipButton: some View {
        CuppedButton(
            title: "Maybe Later",
            style: .text
        ) {
            onComplete()
        }
    }

    // MARK: - Footer

    private var footerSection: some View {
        Text("You can change this later in Settings.")
            .font(.cuppedCaption)
            .foregroundStyle(Color.cuppedMuted)
            .multilineTextAlignment(.center)
    }
}

// MARK: - Previews

#Preview("Face ID Prompt") {
    BiometricPromptView {
        print("Biometric prompt completed")
    }
}
