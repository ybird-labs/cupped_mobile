import SwiftUI

enum GamificationType {
    case xp(Int)
    case streak(Int)
    case badge(String)

    var icon: String {
        switch self {
        case .xp: "star.fill"
        case .streak: "flame.fill"
        case .badge: "medal.fill"
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
        case .xp(let points): "\(points) XP"
        case .streak(let days): "\(days) day streak"
        case .badge(let name): name
        }
    }
}

struct GamificationBadge: View {
    let type: GamificationType

    var body: some View {
        HStack(spacing: Spacing.xs) {
            Image(systemName: type.icon)
                .font(.caption)
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
