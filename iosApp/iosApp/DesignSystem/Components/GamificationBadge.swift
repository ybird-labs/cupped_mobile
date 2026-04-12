import SwiftUI

enum GamificationType {
    case xp(Int)
    case streak(Int)
    case badge(String)

    var icon: AppIcon {
        switch self {
        case .xp: .rating
        case .streak: .streak
        case .badge: .badge
        }
    }

    var color: Color {
        switch self {
        case .xp: .cuppedXP
        case .streak: .cuppedStreak
        case .badge: .cuppedBadge
        }
    }

    var label: String {
        switch self {
        case .xp(let points):
            let clamped = max(0, points)
            return "\(clamped) XP"
        case .streak(let days):
            let clamped = max(0, days)
            return "\(clamped) \(clamped == 1 ? "day" : "days") streak"
        case .badge(let name): return name
        }
    }
}

struct GamificationBadge: View {
    let type: GamificationType

    var body: some View {
        HStack(spacing: Spacing.xs) {
            AppIconView(icon: type.icon, size: 12, color: type.color)
            Text(type.label)
                .font(.cuppedCaption)
                .fontWeight(.semibold)
        }
        .foregroundStyle(type.color)
        .padding(.horizontal, Spacing.sm)
        .padding(.vertical, Spacing.xs)
        .background(type.color.opacity(0.12))
        .clipShape(Capsule())
    }
}

#Preview("Gamification Badges") {
    HStack(spacing: Spacing.sm) {
        GamificationBadge(type: .xp(150))
        GamificationBadge(type: .streak(7))
        GamificationBadge(type: .badge("First Brew"))
    }
    .padding()
}
