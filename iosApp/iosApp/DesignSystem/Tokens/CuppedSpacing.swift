import SwiftUI
import Shared

enum Spacing {
    static let xs = CGFloat(CuppedSpacing.shared.xs)
    static let sm = CGFloat(CuppedSpacing.shared.sm)
    static let md = CGFloat(CuppedSpacing.shared.md)
    static let base = CGFloat(CuppedSpacing.shared.base)
    static let lg = CGFloat(CuppedSpacing.shared.lg)
    static let xl = CGFloat(CuppedSpacing.shared.xl)
    static let xxl = CGFloat(CuppedSpacing.shared.xxl)
}

/// Corner radii for the Cupped design system.
/// Always use `style: .continuous` with `RoundedRectangle` for Apple's squircle curves.
enum Radius {
    static let sm = CGFloat(CuppedSpacing.shared.radiusSm)
    static let md = CGFloat(CuppedSpacing.shared.radiusMd)
    static let lg = CGFloat(CuppedSpacing.shared.radiusLg)
    static let xl = CGFloat(CuppedSpacing.shared.radiusXl)  // 24pt — large cards, sheets
    static let full = CGFloat(CuppedSpacing.shared.radiusFull)
}
