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
    var icon: String? = nil
    var isLoading: Bool = false
    var isDisabled: Bool = false
    let action: () -> Void

    /// Gray bg + muted text when disabled or loading.
    /// React spec: loading = `bg-canvas-border text-ink-muted` with white spinner.
    private var isVisuallyDisabled: Bool {
        isDisabled || isLoading
    }

    /// Can't tap when disabled OR loading.
    private var isInteractionDisabled: Bool {
        isDisabled || isLoading
    }

    var body: some View {
        Button(action: action) {
            HStack(spacing: Spacing.sm) {
                if isLoading {
                    ProgressView()
                        .tint(.white)
                }
                Text(title)
                    .font(.cuppedText(size: 18, weight: .bold))
                if let icon, !isLoading {
                    Image(systemName: icon)
                        .font(.system(size: 15, weight: .bold))
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
                        .strokeBorder(Color.cuppedInk.opacity(0.15), lineWidth: 1)
                }
            }
        }
        .shadow(
            color: shouldShowShadow
                ? Color.cuppedPrimary.opacity(0.3)
                : .clear,
            radius: 10,
            x: 0,
            y: 4
        )
        .buttonStyle(TapScaleButtonStyle())
        .disabled(isInteractionDisabled)
    }

    // MARK: - Resolved Colors

    /// Background color accounting for disabled state.
    /// React spec: disabled = `bg-canvas-border text-ink-muted`
    private var resolvedBackground: Color {
        if isVisuallyDisabled {
            switch style {
            case .primary, .secondary: return .cuppedCanvasBorder
            case .tertiary: return .cuppedCard
            case .text: return .clear
            }
        }
        return activeBackground
    }

    /// Foreground color accounting for disabled state.
    private var resolvedForeground: Color {
        if isVisuallyDisabled {
            switch style {
            case .primary, .secondary: return .cuppedMuted
            case .tertiary: return .cuppedMuted
            case .text: return .cuppedMuted
            }
        }
        return activeForeground
    }

    /// Active (enabled) background color.
    private var activeBackground: Color {
        switch style {
        case .primary: .cuppedPrimary
        case .secondary: .cuppedInk
        case .tertiary: .cuppedCard
        case .text: .clear
        }
    }

    /// Active (enabled) foreground color.
    private var activeForeground: Color {
        switch style {
        case .primary, .secondary: .white
        case .tertiary: .cuppedInk
        case .text: .cuppedPrimary
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
        CuppedButton(title: "Sign in", style: .primary, icon: "arrow.right") {}
        CuppedButton(title: "Secondary", style: .secondary) {}
        CuppedButton(title: "Tertiary", style: .tertiary) {}
        CuppedButton(title: "Text Button", style: .text) {}
        CuppedButton(title: "Loading", style: .primary, isLoading: true) {}
        CuppedButton(title: "Disabled", style: .primary, icon: "arrow.right", isDisabled: true) {}
    }
    .padding()
}
