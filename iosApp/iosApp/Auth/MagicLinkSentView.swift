import SwiftUI
import Shared

// MARK: - MagicLinkSentView

/// Success state after magic link is sent — faithful SwiftUI translation
/// of the React SuccessState component from AuthPage.tsx.
///
/// ## Layout (top to bottom)
/// 1. Mail icon in primary-light bg with primary/20 border (64×64pt)
/// 2. "Your link is on its way." / "Magic link sent." heading
/// 3. "We emailed a sign-in link to" + email in bold
/// 4. Single instruction card with 15-minute expiry notice
/// 5. Community trust line (register only)
/// 6. "Wrong email? Try again →" link
/// 7. Spam folder reminder footer
struct MagicLinkSentView: View {

    /// The email address the magic link was sent to.
    let email: String

    /// Whether the user was in register mode (affects heading/instruction text).
    let isRegisterMode: Bool

    /// Called when the user taps "Wrong email? Try again".
    let onReset: () -> Void

    // MARK: - State

    @State private var iconAppeared = false

    // MARK: - Body

    var body: some View {
        VStack(spacing: 0) {
            // 1. Mail icon — React: bg-primary-light, border-primary/20
            mailIcon
                .padding(.bottom, Spacing.xl) // mb-6 = 24px

            // 2. Heading
            // React: text-2xl font-serif font-bold, delay 0.2
            headingSection
                .padding(.bottom, Spacing.sm) // mb-2 = 8px

            // 3. Subtitle + email
            // React: text-ink-secondary text-sm + font-semibold text-ink, mb-6
            emailSection
                .padding(.bottom, Spacing.xl) // mb-6 = 24px

            // 4. Single instruction card
            // React: bg-canvas rounded-xl border px-6 py-4, mb-6
            instructionCard
                .padding(.bottom, Spacing.xl) // mb-6 = 24px

            // 5. Community trust line (register only)
            // React: flex items-center gap-2, mb-6
            if isRegisterMode {
                communityLine
                    .padding(.bottom, Spacing.xl) // mb-6 = 24px
            }

            // 6. Reset button
            // React: text-sm text-ink-muted hover:text-ink
            resetButton

            // 7. Footer
            // React: text-xs text-ink-muted/60 mt-4
            footerText
                .padding(.top, Spacing.base) // mt-4 = 16px
        }
    }

    // MARK: - Mail Icon

    /// React: bg-primary-light rounded-2xl border border-primary/20
    /// with Mail icon in primary color.
    private var mailIcon: some View {
        RoundedRectangle(cornerRadius: Radius.lg, style: .continuous)
            .fill(Color.cuppedPrimaryLight)
            .frame(width: 64, height: 64)
            .overlay {
                RoundedRectangle(cornerRadius: Radius.lg, style: .continuous)
                    .strokeBorder(
                        Color.cuppedPrimary.opacity(0.2),
                        lineWidth: 1
                    )
            }
            .overlay {
                Image(systemName: "envelope")
                    .font(.system(size: 32))
                    .foregroundStyle(Color.cuppedPrimary)
            }
            .scaleEffect(iconAppeared ? 1 : 0)
            .offset(y: iconAppeared ? 0 : 10)
            .onAppear {
                withAnimation(
                    .spring(response: 0.5, dampingFraction: 0.65)
                        .delay(0.1)
                ) {
                    iconAppeared = true
                }
            }
    }

    // MARK: - Heading

    /// React: "Your link is on its way." (register) / "Magic link sent." (login)
    private var headingSection: some View {
        Text(isRegisterMode ? "Your link is on its way." : "Magic link sent.")
            .font(.cuppedTitle3)
            .fontWeight(.bold)
            .foregroundStyle(Color.cuppedInk)
            .multilineTextAlignment(.center)
            .opacity(iconAppeared ? 1 : 0)
            .offset(y: iconAppeared ? 0 : 8)
            .animation(
                .timingCurve(0.16, 1, 0.3, 1, duration: 0.4).delay(0.2),
                value: iconAppeared
            )
    }

    // MARK: - Email Section

    /// React: "We emailed a sign-in link to" + bold email
    private var emailSection: some View {
        VStack(spacing: Spacing.xs) {
            Text("We emailed a sign-in link to")
                .font(.cuppedSubheadline)
                .foregroundStyle(Color.cuppedSecondary)

            Text(email)
                .font(.cuppedBody)
                .fontWeight(.semibold)
                .foregroundStyle(Color.cuppedInk)
        }
        .opacity(iconAppeared ? 1 : 0)
        .offset(y: iconAppeared ? 0 : 8)
        .animation(
            .timingCurve(0.16, 1, 0.3, 1, duration: 0.4).delay(0.2),
            value: iconAppeared
        )
    }

    // MARK: - Instruction Card

    /// React: Single card — "Open the email and click the link to
    /// activate your account / sign in. It expires in **15 minutes**."
    private var instructionCard: some View {
        let actionText = isRegisterMode
            ? "activate your account"
            : "sign in"

        return HStack(spacing: 0) {
            (
                Text("Open the email and click the link to \(actionText). It expires in ")
                    .foregroundStyle(Color.cuppedSecondary)
                + Text("15 minutes")
                    .fontWeight(.semibold)
                    .foregroundStyle(Color.cuppedInk)
                + Text(".")
                    .foregroundStyle(Color.cuppedSecondary)
            )
            .font(.cuppedSubheadline)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, Spacing.xl)  // React: px-6 = 24px
        .padding(.vertical, Spacing.base)   // React: py-4 = 16px
        .background(Color.cuppedCanvas)
        .clipShape(
            RoundedRectangle(
                cornerRadius: Radius.lg, // React: rounded-xl
                style: .continuous
            )
        )
        .overlay {
            RoundedRectangle(
                cornerRadius: Radius.lg,
                style: .continuous
            )
            .strokeBorder(Color.cuppedCanvasBorder, lineWidth: 1)
        }
        .opacity(iconAppeared ? 1 : 0)
        .offset(y: iconAppeared ? 0 : 8)
        .animation(
            .timingCurve(0.16, 1, 0.3, 1, duration: 0.4).delay(0.3),
            value: iconAppeared
        )
    }

    // MARK: - Community Trust Line (Register Only)

    /// React: Users icon + "Trusted by coffee lovers worldwide"
    private var communityLine: some View {
        HStack(spacing: Spacing.sm) {
            Image(systemName: "person.2")
                .font(.system(size: 14))
                .foregroundStyle(Color.cuppedMuted)

            Text("Trusted by coffee lovers worldwide")
                .font(.cuppedSubheadline)
                .foregroundStyle(Color.cuppedMuted)
        }
        .opacity(iconAppeared ? 1 : 0)
        .offset(y: iconAppeared ? 0 : 8)
        .animation(
            .timingCurve(0.16, 1, 0.3, 1, duration: 0.4).delay(0.4),
            value: iconAppeared
        )
    }

    // MARK: - Reset Button

    /// React: "Wrong email? Try again →"
    private var resetButton: some View {
        Button(action: onReset) {
            Text("Wrong email? Try again →")
                .font(.cuppedSubheadline)
                .foregroundStyle(Color.cuppedMuted)
        }
        .opacity(iconAppeared ? 1 : 0)
        .animation(
            .easeOut(duration: 0.3).delay(0.5),
            value: iconAppeared
        )
    }

    // MARK: - Footer

    /// React: "Can't find it? Check your spam folder."
    private var footerText: some View {
        Text("Can't find it? Check your spam folder.")
            .font(.cuppedCaption)
            .foregroundStyle(Color.cuppedMuted.opacity(0.6))
            .multilineTextAlignment(.center)
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
        .padding(Spacing.xxl)
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
        .padding(Spacing.xxl)
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
