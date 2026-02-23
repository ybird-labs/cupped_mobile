import SwiftUI

/// Shadow tokens matching the Cupped Design System.
/// Derived from Magic Patterns tailwind.config.js boxShadow values.
///
/// Usage:
///   .modifier(Shadow.warm)
///   .modifier(Shadow.warmLg)
enum Shadow {
    // MARK: - Warm (neutral ink-based shadows)

    /// Subtle card shadow — default for CuppedCard
    static let warm = ShadowModifier(
        lightOpacity: 0.05,
        darkOpacity: 0.3,
        radius: 6,
        x: 0,
        y: 4
    )

    /// Medium elevation — dropdowns, popovers
    static let warmLg = ShadowModifier(
        lightOpacity: 0.08,
        darkOpacity: 0.4,
        radius: 15,
        x: 0,
        y: 10
    )

    /// High elevation — modals, sheets
    static let warmXl = ShadowModifier(
        lightOpacity: 0.1,
        darkOpacity: 0.5,
        radius: 25,
        x: 0,
        y: 20
    )

    // MARK: - Glow (colored shadows for gamification)

    /// Coral glow — FAB, primary action emphasis
    static let glowCoral = GlowModifier(
        color: .cuppedPrimary,
        opacity: 0.4,
        radius: 20
    )

    /// XP gold glow — achievement celebrations
    static let glowXp = GlowModifier(
        color: .cuppedXP,
        opacity: 0.4,
        radius: 20
    )
}

/// A ViewModifier that applies a single ink-based shadow layer,
/// adapting opacity for light/dark mode.
struct ShadowModifier: ViewModifier {
    @Environment(\.colorScheme) var colorScheme

    let lightOpacity: Double
    let darkOpacity: Double
    let radius: CGFloat
    let x: CGFloat
    let y: CGFloat

    func body(content: Content) -> some View {
        let opacity = colorScheme == .dark ? darkOpacity : lightOpacity
        content.shadow(
            color: Color.cuppedInk.opacity(opacity),
            radius: radius,
            x: x,
            y: y
        )
    }
}

/// A ViewModifier that applies a colored glow shadow (non-directional).
struct GlowModifier: ViewModifier {
    let color: Color
    let opacity: Double
    let radius: CGFloat

    func body(content: Content) -> some View {
        content.shadow(color: color.opacity(opacity), radius: radius, x: 0, y: 0)
    }
}
