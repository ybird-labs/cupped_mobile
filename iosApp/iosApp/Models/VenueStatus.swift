import SwiftUI

enum VenueStatus: String, Hashable {
    case basic
    case claimed
    case curated

    var foregroundColor: Color {
        switch self {
        case .basic:   .white
        case .claimed: .cuppedInfo
        case .curated: .cuppedPrimary
        }
    }

    var backgroundColor: Color {
        switch self {
        case .basic:   Color.cuppedMuted.opacity(0.9)
        case .claimed: Color.cuppedInfoLight.opacity(0.95)
        case .curated: Color.cuppedPrimaryLight.opacity(0.95)
        }
    }

    var borderColor: Color {
        switch self {
        case .basic:   Color.cuppedMuted.opacity(0.3)
        case .claimed: Color.cuppedInfo.opacity(0.2)
        case .curated: Color.cuppedPrimary.opacity(0.2)
        }
    }
}

#Preview("Venue Status Tiers") {
    HStack(spacing: Spacing.md) {
        ForEach([VenueStatus.basic, .claimed, .curated], id: \.self) { status in
            HStack(spacing: Spacing.xs) {
                Image(systemName: "mappin")
                    .font(.caption2)
                Text("Blue Bottle")
                    .font(.cuppedCaption)
                    .fontWeight(.medium)
            }
            .foregroundStyle(status.foregroundColor)
            .padding(.horizontal, Spacing.sm)
            .padding(.vertical, Spacing.xs)
            .background(status.backgroundColor)
            .overlay(
                Capsule()
                    .strokeBorder(status.borderColor, lineWidth: 1)
            )
            .clipShape(Capsule())
        }
    }
    .padding()
}
