import SwiftUI

enum BrewMethod: String, CaseIterable, Hashable {
    case v60, chemex, aeropress, frenchPress, espresso
    case flatWhite, latte, cappuccino, americano
    case coldBrew, drip, pourOver

    var icon: String {
        switch self {
        case .v60:         "line.3.crossed.swirl.circle"
        case .chemex:      "flask"
        case .aeropress:   "arrow.down.to.line"
        case .frenchPress: "cup.and.saucer"
        case .espresso:    "cup.and.saucer.fill"
        case .flatWhite:   "mug.fill"
        case .latte:       "mug"
        case .cappuccino:  "cup.and.heat.waves.fill"
        case .americano:   "drop.fill"
        case .coldBrew:    "snowflake"
        case .drip:        "drop.degreesign"
        case .pourOver:    "arrow.down.circle"
        }
    }

    var label: String {
        switch self {
        case .v60:         "V60"
        case .chemex:      "Chemex"
        case .aeropress:   "AeroPress"
        case .frenchPress: "French Press"
        case .espresso:    "Espresso"
        case .flatWhite:   "Flat White"
        case .latte:       "Latte"
        case .cappuccino:  "Cappuccino"
        case .americano:   "Americano"
        case .coldBrew:    "Cold Brew"
        case .drip:        "Drip"
        case .pourOver:    "Pour Over"
        }
    }
}

#Preview("Brew Methods") {
    ScrollView {
        FlowLayout(spacing: Spacing.sm) {
            ForEach(BrewMethod.allCases, id: \.self) { method in
                HStack(spacing: Spacing.xs) {
                    Image(systemName: method.icon)
                        .font(.caption2)
                    Text(method.label)
                        .font(.cuppedCaption)
                        .fontWeight(.medium)
                }
                .foregroundStyle(.white)
                .padding(.horizontal, Spacing.sm)
                .padding(.vertical, Spacing.xs)
                .background(Color.black.opacity(0.7))
                .clipShape(Capsule())
            }
        }
        .padding()
    }
}
