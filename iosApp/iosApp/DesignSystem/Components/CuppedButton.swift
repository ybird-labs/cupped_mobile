import SwiftUI

enum CuppedButtonStyle {
    case primary
    case secondary
    case tertiary
    case text
}

struct CuppedButton: View {
    let title: String
    let style: CuppedButtonStyle
    var icon: AppIcon? = nil
    var isLoading: Bool = false
    var isDisabled: Bool = false
    let action: () -> Void

    /// Whether the button appears visually disabled (muted
    /// colors). Currently identical to `isInteractionDisabled`
    /// but kept separate: visual state could diverge in the
    /// future (e.g., a "pending" state that looks active but
    /// blocks taps).
    private var isDisabledOrLoading: Bool {
        isDisabled || isLoading
    }

    private var isVisuallyDisabled: Bool { isDisabledOrLoading }
    private var isInteractionDisabled: Bool { isDisabledOrLoading }

    var body: some View {
        Button(action: action) {
            HStack(spacing: Spacing.sm) {
                if isLoading {
                    ProgressView()
                        .tint(activeForeground)
                }
                Text(title)
                    .font(.cuppedText(size: 18, weight: .bold))
                if let icon, !isLoading {
                    AppIconView(icon: icon, size: 15, color: resolvedForeground)
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, Spacing.base)
            .padding(.horizontal, Spacing.lg)
            .background(resolvedBackground)
            .foregroundStyle(resolvedForeground)
            .clipShape(RoundedRectangle(cornerRadius: Radius.lg, style: .continuous))
            .overlay {
                if style == .tertiary {
                    RoundedRectangle(cornerRadius: Radius.lg, style: .continuous)
                        .strokeBorder(Color.cuppedTextPrimary.opacity(0.15), lineWidth: 1)
                }
            }
        }
        .shadow(
            color: shouldShowShadow
                ? Color.cuppedActionPrimary.opacity(0.3)
                : .clear,
            radius: 10,
            x: 0,
            y: 4
        )
        .buttonStyle(TapScaleButtonStyle())
        .disabled(isInteractionDisabled)
        .accessibilityLabel(title)
        .accessibilityValue(isLoading ? "Loading" : "")
    }

    // MARK: - Resolved Colors

    /// Background color accounting for disabled state.
    /// React spec: disabled = `bg-canvas-border text-ink-muted`
    private var resolvedBackground: Color {
        if isVisuallyDisabled {
            switch style {
            case .primary, .secondary: return .cuppedBorderDefault
            case .tertiary: return .cuppedSurfaceCard
            case .text: return .clear
            }
        }
        return activeBackground
    }

    /// Foreground color accounting for disabled state.
    private var resolvedForeground: Color {
        if isVisuallyDisabled { return .cuppedTextMuted }
        return activeForeground
    }

    /// Active (enabled) background color.
    private var activeBackground: Color {
        switch style {
        case .primary: .cuppedActionPrimary
        case .secondary: .cuppedTextPrimary
        case .tertiary: .cuppedSurfaceCard
        case .text: .clear
        }
    }

    /// Active (enabled) foreground color.
    private var activeForeground: Color {
        switch style {
        case .primary, .secondary: .white
        case .tertiary: .cuppedTextPrimary
        case .text: .cuppedActionPrimary
        }
    }

    /// Coral glow shadow only on enabled primary buttons.
    private var shouldShowShadow: Bool {
        style == .primary && !isVisuallyDisabled
    }
}

struct TapScaleButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? Motion.tapScale : 1)
            .opacity(configuration.isPressed ? Motion.tapOpacity : 1)
            .animation(.cuppedSpring, value: configuration.isPressed)
    }
}

#Preview("Button Styles") {
    VStack(spacing: Spacing.base) {
        CuppedButton(title: "Sign in", style: .primary, icon: .forward) {}
        CuppedButton(title: "Secondary", style: .secondary) {}
        CuppedButton(title: "Tertiary", style: .tertiary) {}
        CuppedButton(title: "Text Button", style: .text) {}
        CuppedButton(title: "Loading", style: .primary, isLoading: true) {}
        CuppedButton(title: "Disabled", style: .primary, icon: .forward, isDisabled: true) {}
    }
    .padding()
}
