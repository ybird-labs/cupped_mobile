import SwiftUI
import Shared

// MARK: - MagicLinkSentView

/// Success state after magic link is sent — "Check your inbox!" screen.
///
/// Faithfully translates the SuccessState component from the Magic Patterns
/// AuthPage.tsx design. Shows a green checkmark, the email address, numbered
/// instructions, and options to use a different email.
///
/// ## Layout (top to bottom)
/// 1. Green circle with checkmark icon (64×64pt)
/// 2. "Check your inbox!" heading
/// 3. "We sent a magic link to" + email in bold
/// 4. Instructions card with 3 numbered steps (coral badges)
/// 5. "Use a different email →" link
/// 6. Spam folder reminder footer
struct MagicLinkSentView: View {

    /// The email address the magic link was sent to.
    let email: String

    /// Whether the user was in register mode (affects step text).
    let isRegisterMode: Bool

    /// Called when the user taps "Use a different email".
    let onReset: () -> Void

    // MARK: - State

    @State private var checkmarkAppeared = false

    // MARK: - Instruction Steps

    private var steps: [(number: Int, text: String)] {
        [
            (1, "Open the email we just sent you"),
            (
                2,
                isRegisterMode
                    ? "Click the magic link to activate your account"
                    : "Click the magic link to sign in"
            ),
            (
                3,
                isRegisterMode
                    ? "Set up your profile and start tracking!"
                    : "Continue your coffee journey!"
            ),
        ]
    }

    // MARK: - Body

    var body: some View {
        VStack(spacing: Spacing.lg) {
            // 1. Success icon
            successIcon

            // 2. Heading
            Text("Check your inbox!")
                .font(.cuppedTitle2)
                .fontWeight(.bold)
                .foregroundStyle(Color.cuppedInk)
                .multilineTextAlignment(.center)

            // 3. Subtitle + email
            VStack(spacing: Spacing.xs) {
                Text("We sent a magic link to")
                    .font(.cuppedBody)
                    .foregroundStyle(Color.cuppedSecondary)

                Text(email)
                    .font(.cuppedBody)
                    .fontWeight(.medium)
                    .foregroundStyle(Color.cuppedInk)
            }

            // 4. Instructions card
            instructionsCard

            // 5. Different email button
            Button(action: onReset) {
                HStack(spacing: Spacing.xs) {
                    Text("Use a different email")
                    Image(systemName: "arrow.right")
                        .font(.system(size: 12, weight: .medium))
                }
                .font(.cuppedSubheadline)
                .foregroundStyle(Color.cuppedMuted)
            }

            // 6. Footer
            Text(
                "Didn't receive the email? Check your spam folder or try again."
            )
            .font(.cuppedCaption)
            .foregroundStyle(Color.cuppedMuted)
            .multilineTextAlignment(.center)
        }
    }

    // MARK: - Success Icon

    private var successIcon: some View {
        Circle()
            .fill(Color.cuppedSuccess)
            .frame(width: 64, height: 64)
            .overlay {
                Image(systemName: "checkmark")
                    .font(.system(size: 32, weight: .bold))
                    .foregroundStyle(.white)
            }
            .scaleEffect(checkmarkAppeared ? 1 : 0)
            .onAppear {
                withAnimation(
                    .spring(response: 0.5, dampingFraction: 0.65)
                ) {
                    checkmarkAppeared = true
                }
            }
    }

    // MARK: - Instructions Card

    private var instructionsCard: some View {
        VStack(alignment: .leading, spacing: Spacing.md) {
            Text("What's next?")
                .font(.cuppedSubheadline)
                .fontWeight(.medium)
                .foregroundStyle(Color.cuppedSecondary)

            ForEach(
                Array(steps.enumerated()),
                id: \.offset
            ) { index, step in
                stepRow(number: step.number, text: step.text)
                    .opacity(checkmarkAppeared ? 1 : 0)
                    .offset(y: checkmarkAppeared ? 0 : 8)
                    .animation(
                        .cuppedSpring.delay(
                            Double(index) * Motion.staggerDelay
                        ),
                        value: checkmarkAppeared
                    )
            }
        }
        .padding(Spacing.md)
        .background(
            Color.cuppedCanvas.opacity(0.5)
        )
        .clipShape(
            RoundedRectangle(
                cornerRadius: Radius.md,
                style: .continuous
            )
        )
    }

    // MARK: - Step Row

    private func stepRow(number: Int, text: String) -> some View {
        HStack(spacing: Spacing.sm) {
            // Coral number badge
            Text("\(number)")
                .font(.cuppedCaption)
                .fontWeight(.bold)
                .foregroundStyle(Color.cuppedPrimary)
                .frame(width: 24, height: 24)
                .background(
                    Circle()
                        .fill(
                            Color.cuppedPrimary.opacity(0.1)
                        )
                )

            Text(text)
                .font(.cuppedSubheadline)
                .foregroundStyle(Color.cuppedSecondary)
        }
    }
}

// MARK: - Previews

#Preview("Login Mode") {
    ZStack {
        Color.cuppedCanvas.ignoresSafeArea()

        MagicLinkSentView(
            email: "coffee@cupped.cafe",
            isRegisterMode: false,
            onReset: {}
        )
        .padding(Spacing.xl)
        .background(Color.cuppedCard)
        .clipShape(
            RoundedRectangle(
                cornerRadius: Radius.xl,
                style: .continuous
            )
        )
        .modifier(Shadow.warmXl)
        .frame(maxWidth: 400)
        .padding()
    }
}

#Preview("Register Mode") {
    ZStack {
        Color.cuppedCanvas.ignoresSafeArea()

        MagicLinkSentView(
            email: "newbrewer@cupped.cafe",
            isRegisterMode: true,
            onReset: {}
        )
        .padding(Spacing.xl)
        .background(Color.cuppedCard)
        .clipShape(
            RoundedRectangle(
                cornerRadius: Radius.xl,
                style: .continuous
            )
        )
        .modifier(Shadow.warmXl)
        .frame(maxWidth: 400)
        .padding()
    }
}
