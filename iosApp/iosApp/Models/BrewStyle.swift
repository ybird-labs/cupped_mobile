import SwiftUI

enum BrewStyle: String, CaseIterable, Hashable {
    case drip
    case batch
    case latte
    case immersion
    case coldBrew
    

    var feedBadgeIcon: AppIcon {
        switch self {
        case .drip:
            .coffeeChemex
        case .batch:
            .coffeePot
        case .immersion:
            .coffeeAeropress
        case .latte:
            .coffee
        case .coldBrew:
            .coffeeTogo
        }
    }

    var label: String {
        switch self {
            case .drip:
            "Drip"
        case .batch:
            "Batch"
        case .immersion:
            "Immersion"
        case .latte:
            "Latte"
        case .coldBrew:
            "Cold Brew"
  
        }
    }

}

#Preview("Brew Methods") {
    ScrollView {
        FlowLayout(spacing: Spacing.sm) {
            ForEach(BrewStyle.allCases, id: \.self) { method in
                HStack(spacing: Spacing.xs) {
                    AppIconView(icon: method.feedBadgeIcon, size: 12, color: .white)
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
