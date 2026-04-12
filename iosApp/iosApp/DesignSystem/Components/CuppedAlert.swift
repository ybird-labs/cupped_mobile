import SwiftUI

enum AlertVariant {
    case success
    case error
    case warning
    case info

    var color: Color {
        switch self {
        case .success: .cuppedSuccess
        case .error: .cuppedError
        case .warning: .cuppedWarning
        case .info: .cuppedInfo
        }
    }

    var icon: AppIcon {
        switch self {
        case .success: .success
        case .error: .error
        case .warning: .warning
        case .info: .info
        }
    }
}

struct CuppedAlert: View {
    let variant: AlertVariant
    let title: String
    var message: String?

    var body: some View {
        HStack(alignment: .top, spacing: Spacing.md) {
            AppIconView(icon: variant.icon, size: 20, color: variant.color)

            VStack(alignment: .leading, spacing: Spacing.xs) {
                Text(title)
                    .font(.cuppedText(size: 14, weight: .semibold))
                    .foregroundStyle(Color.cuppedInk)

                if let message {
                    Text(message)
                        .font(.cuppedSubheadline)
                        .foregroundStyle(Color.cuppedSecondary)
                }
            }

            Spacer(minLength: 0)
        }
        .padding(Spacing.base)
        .background(variant.color.opacity(0.08))
        .clipShape(RoundedRectangle(cornerRadius: Radius.md, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: Radius.md, style: .continuous)
                .strokeBorder(variant.color.opacity(0.3), lineWidth: 1)
        }
    }
}

#Preview("Alerts") {
    VStack(spacing: Spacing.base) {
        CuppedAlert(variant: .success, title: "Brew logged!", message: "You earned 15 XP.")
        CuppedAlert(variant: .error, title: "Connection failed", message: "Check your internet.")
        CuppedAlert(variant: .warning, title: "Low beans", message: "Time to restock.")
        CuppedAlert(variant: .info, title: "New feature", message: "Try the flavor wheel.")
    }
    .padding()
}
