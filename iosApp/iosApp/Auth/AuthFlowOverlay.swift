import SwiftUI

struct AuthFlowOverlay: View {
    @Environment(AuthCoordinator.self) private var authCoordinator

    private var showsAuthDebugDetails: Bool {
        let bundleId = Bundle.main.bundleIdentifier ?? ""
        return bundleId != "cafe.cupped.app"
    }

    var body: some View {
        switch authCoordinator.authFlowStatus {
        case .idle:
            EmptyView()

        case .verifyingMagicLink:
            progressPanel(
                title: "Verifying your magic link...",
                message: "We received your sign-in link and are validating it now."
            )

        case .establishingSession:
            progressPanel(
                title: "Signing you in...",
                message: "Your link is valid. We are establishing your secure session."
            )

        case .succeeded(let message):
            alertContainer {
                CuppedAlert(
                    variant: .success,
                    title: "Magic link sign-in complete",
                    message: message
                )
            }

        case .failed(let message, let debugDetails):
            alertContainer {
                VStack(alignment: .leading, spacing: Spacing.sm) {
                    CuppedAlert(
                        variant: .error,
                        title: "Magic link sign-in failed",
                        message: message
                    )

                    if showsAuthDebugDetails,
                       let debugDetails,
                       !debugDetails.isEmpty {
                        Text(debugDetails)
                            .font(.cuppedCaption)
                            .foregroundStyle(Color.cuppedTextMuted)
                            .textSelection(.enabled)
                            .padding(.horizontal, Spacing.xs)
                    }

                    Button("Dismiss") {
                        authCoordinator.clearAuthFlowStatus()
                    }
                    .font(.cuppedSubheadline)
                    .fontWeight(.medium)
                    .foregroundStyle(Color.cuppedActionPrimary)
                    .padding(.horizontal, Spacing.xs)
                }
            }
        }
    }

    private func alertContainer<Content: View>(
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack {
            content()
        }
        .frame(maxWidth: 480, alignment: .leading)
        .padding(.horizontal, Spacing.lg)
        .padding(.top, Spacing.lg)
        .transition(.move(edge: .top).combined(with: .opacity))
    }

    private func progressPanel(
        title: String,
        message: String
    ) -> some View {
        HStack(alignment: .center, spacing: Spacing.md) {
            ProgressView()
                .tint(.cuppedInfo)

            VStack(alignment: .leading, spacing: Spacing.xs) {
                Text(title)
                    .font(.cuppedText(size: 14, weight: .semibold))
                    .foregroundStyle(Color.cuppedTextPrimary)

                Text(message)
                    .font(.cuppedSubheadline)
                    .foregroundStyle(Color.cuppedTextSecondary)
            }

            Spacer(minLength: 0)
        }
        .padding(Spacing.base)
        .background(Color.cuppedStatusInfoBackground.opacity(0.5))
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
            .strokeBorder(Color.cuppedStatusInfoForeground.opacity(0.25), lineWidth: 1)
        }
        .frame(maxWidth: 480, alignment: .leading)
        .padding(.horizontal, Spacing.lg)
        .padding(.top, Spacing.lg)
        .transition(.move(edge: .top).combined(with: .opacity))
    }
}

#Preview("Magic Link Progress") {
    ZStack(alignment: .top) {
        Color.cuppedSurfaceApp.ignoresSafeArea()
        AuthFlowOverlay()
    }
    .environment({
        let coordinator = AuthCoordinator()
        coordinator.authFlowStatus = .verifyingMagicLink
        return coordinator
    }())
}

#Preview("Magic Link Success") {
    ZStack(alignment: .top) {
        Color.cuppedSurfaceApp.ignoresSafeArea()
        AuthFlowOverlay()
    }
    .environment({
        let coordinator = AuthCoordinator()
        coordinator.authFlowStatus = .succeeded(
            message: "Magic link verified. You're signed in."
        )
        return coordinator
    }())
}

#Preview("Magic Link Failure") {
    ZStack(alignment: .top) {
        Color.cuppedSurfaceApp.ignoresSafeArea()
        AuthFlowOverlay()
    }
    .environment({
        let coordinator = AuthCoordinator()
        coordinator.authFlowStatus = .failed(
            message: "This magic link expired or is invalid. Request a new one.",
            debugDetails: "Invalid or expired token"
        )
        return coordinator
    }())
}
