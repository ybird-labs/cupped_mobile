import SwiftUI
import UIKit

enum BrewMethod: String, CaseIterable, Hashable {
    case v60, chemex, aeropress, frenchPress, espresso
    case flatWhite, latte, cappuccino, americano
    case coldBrew, drip, pourOver

    var icon: String {
        Self.supportedSymbolName(from: iconCandidates)
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

    private var iconCandidates: [String] {
        switch self {
        case .v60:         ["line.3.crossed.swirl.circle", "arrow.down.circle", "line.3.crossed.swirl.circle.fill"]
        case .chemex:      ["flask", "cup.and.saucer"]
        case .aeropress:   ["arrow.down.to.line", "arrow.down.circle", "arrow.down"]
        case .frenchPress: ["cup.and.saucer", "mug"]
        case .espresso:    ["cup.and.saucer.fill", "mug.fill"]
        case .flatWhite:   ["mug.fill", "cup.and.saucer.fill"]
        case .latte:       ["mug", "cup.and.saucer"]
        case .cappuccino:  ["cup.and.heat.waves.fill", "cup.and.saucer.fill", "mug.fill"]
        case .americano:   ["drop.fill", "drop"]
        case .coldBrew:    ["snowflake", "drop.fill"]
        case .drip:        ["drop.degreesign", "drop.fill", "drop"]
        case .pourOver:    ["arrow.down.circle", "arrow.down.to.line", "arrow.down"]
        }
    }

    private static func supportedSymbolName(from candidates: [String]) -> String {
        for name in candidates where UIImage(systemName: name) != nil {
            return name
        }
        return "questionmark.circle"
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
