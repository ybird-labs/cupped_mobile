import SwiftUI

enum FlavorNote: String, CaseIterable {
    case fruity, floral, nutty, chocolate, spice
    case sweet, citrus, green, berry, roasted

    var color: Color {
        switch self {
        case .fruity: .cuppedFruity
        case .floral: .cuppedFloral
        case .nutty: .cuppedNutty
        case .chocolate: .cuppedChocolate
        case .spice: .cuppedSpice
        case .sweet: .cuppedSweet
        case .citrus: .cuppedCitrus
        case .green: .cuppedGreen
        case .berry: .cuppedBerry
        case .roasted: .cuppedRoasted
        }
    }

    /// Background color guaranteed to meet WCAG AA ≥4.5:1 contrast against white text.
    var accessibleBackground: Color {
        switch self {
        case .fruity: .cuppedFruityAccessible
        case .floral: .cuppedFloralAccessible
        case .citrus: .cuppedCitrusAccessible
        case .nutty: .cuppedNuttyAccessible
        case .green: .cuppedGreenAccessible
        // These already pass WCAG AA against white:
        case .berry: .cuppedBerry
        case .chocolate: .cuppedChocolate
        case .spice: .cuppedSpice
        case .roasted: .cuppedRoasted
        // Sweet uses ink text, not white — use the standard color:
        case .sweet: .cuppedSweet
        }
    }

    /// Text color for use on the solid accessibleBackground.
    var textOnBackground: Color {
        switch self {
        case .sweet: .cuppedInk  // Sweet is too light for white text
        default: .white
        }
    }

    var label: String { rawValue.capitalized }
}

struct FlavorTag: View {
    let flavor: FlavorNote

    var body: some View {
        Text(flavor.label)
            .font(.cuppedCaption)
            .fontWeight(.medium)
            .foregroundStyle(flavor.textOnBackground)
            .padding(.horizontal, Spacing.sm)
            .padding(.vertical, Spacing.xs)
            .background(flavor.accessibleBackground)
            .clipShape(Capsule())
    }
}

#Preview("Flavor Tags") {
    ScrollView {
        FlowLayout(spacing: Spacing.sm) {
            ForEach(FlavorNote.allCases, id: \.self) { flavor in
                FlavorTag(flavor: flavor)
            }
        }
        .padding()
    }
}

// Simple flow layout for tag wrapping
struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let result = arrange(proposal: proposal, subviews: subviews)
        return result.size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = arrange(proposal: proposal, subviews: subviews)
        for (index, position) in result.positions.enumerated() {
            subviews[index].place(
                at: CGPoint(x: bounds.minX + position.x, y: bounds.minY + position.y),
                proposal: .unspecified
            )
        }
    }

    private func arrange(proposal: ProposedViewSize, subviews: Subviews) -> (positions: [CGPoint], size: CGSize) {
        let maxWidth = proposal.width ?? .infinity
        var positions: [CGPoint] = []
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0
        var maxX: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x + size.width > maxWidth, x > 0 {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            positions.append(CGPoint(x: x, y: y))
            rowHeight = max(rowHeight, size.height)
            x += size.width + spacing
            maxX = max(maxX, x - spacing)
        }

        return (positions, CGSize(width: maxX, height: y + rowHeight))
    }
}
