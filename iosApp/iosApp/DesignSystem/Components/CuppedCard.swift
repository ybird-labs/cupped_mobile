import SwiftUI

struct CuppedCard<Content: View>: View {
    // MARK: - Properties
    
    @ViewBuilder let content: () -> Content
    
    /// Controls the padding inside the card
    var padding: CGFloat = Spacing.base
    
    /// Controls the corner radius of the card
    var cornerRadius: CGFloat = Radius.md
    
    /// Enables Liquid Glass effect (modern Apple design)
    /// On iOS 26+: Uses native glassEffect
    /// On earlier versions: Uses ultraThinMaterial as fallback
    var useLiquidGlass: Bool = false
    
    // MARK: - Body
    
    var body: some View {
        Group {
            if useLiquidGlass {
                if #available(iOS 26.0, *) {
                    content()
                        .padding(padding)
                        .glassEffect(in: .rect(cornerRadius: cornerRadius))
                } else {
                    // Fallback glass-like effect for earlier iOS versions
                    content()
                        .padding(padding)
                        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
                        .modifier(Shadow.warmLg)
                }
            } else {
                content()
                    .padding(padding)
                    .background(cardBackground)
                    .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
                    .modifier(Shadow.warm)
            }
        }
    }
    
    // MARK: - Computed Properties
    
    private var cardBackground: Color {
        Color.cuppedCard
    }
}

// MARK: - View Extensions

extension CuppedCard {
    /// Sets custom padding for the card content
    func cardPadding(_ padding: CGFloat) -> CuppedCard {
        var card = self
        card.padding = padding
        return card
    }
    
    /// Sets custom corner radius for the card
    func cardCornerRadius(_ radius: CGFloat) -> CuppedCard {
        var card = self
        card.cornerRadius = radius
        return card
    }
    
    /// Enables the modern Liquid Glass effect
    /// On iOS 26+: Uses native glass effect
    /// On earlier versions: Uses ultraThinMaterial blur as fallback
    func liquidGlass(_ enabled: Bool = true) -> CuppedCard {
        var card = self
        card.useLiquidGlass = enabled
        return card
    }
}

// MARK: - Previews

#Preview("Standard Card") {
    VStack(spacing: Spacing.lg) {
        CuppedCard {
            VStack(alignment: .leading, spacing: Spacing.sm) {
                Text("Ethiopian Yirgacheffe")
                    .font(.cuppedTitle3)
                    .foregroundStyle(Color.cuppedInk)
                Text("Bright, fruity, and floral with a clean finish.")
                    .font(.cuppedBody)
                    .foregroundStyle(Color.cuppedSecondary)
            }
        }
        
        CuppedCard {
            HStack(spacing: Spacing.base) {
                AppIconView(icon: .coffee, size: 40, color: Color.cuppedPrimary)
                
                VStack(alignment: .leading, spacing: Spacing.xs) {
                    Text("Quick Stats")
                        .font(.cuppedSubheadline)
                        .foregroundStyle(Color.cuppedMuted)
                    Text("15 tastings")
                        .font(.cuppedTitle3)
                        .foregroundStyle(Color.cuppedInk)
                }
            }
        }
        .cardPadding(Spacing.lg)
    }
    .padding()
    .background(Color.cuppedCanvas)
}

#Preview("Custom Styles") {
    VStack(spacing: Spacing.lg) {
        // Compact card
        CuppedCard {
            Text("Compact Card")
                .font(.cuppedBody)
        }
        .cardPadding(Spacing.sm)
        .cardCornerRadius(Radius.sm)
        
        // Large rounded card
        CuppedCard {
            VStack(spacing: Spacing.base) {
                AppIconView(icon: .rating, size: 50, color: Color.cuppedPrimary)
                Text("Premium Coffee")
                    .font(.cuppedTitle2)
                    .foregroundStyle(Color.cuppedInk)
            }
            .frame(maxWidth: .infinity)
        }
        .cardPadding(Spacing.xl)
        .cardCornerRadius(Radius.lg)
    }
    .padding()
    .background(Color.cuppedCanvas)
}
#Preview("Liquid Glass Effect") {
    ZStack {
        // Colorful background to show glass effect
        LinearGradient(
            colors: [.cuppedPrimary, .cuppedPrimaryLight, .cuppedFruity],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
        .ignoresSafeArea()
        
        VStack(spacing: Spacing.xl) {
            CuppedCard {
                VStack(alignment: .leading, spacing: Spacing.sm) {
                    Text("Modern Design")
                        .font(.cuppedTitle3)
                    Text("This card uses the Liquid Glass effect")
                        .font(.cuppedBody)
                }
            }
            .liquidGlass()
            
            CuppedCard {
                HStack {
                    AppIconView(icon: .sparkles, size: 30)
                    Text("Interactive Glass")
                        .font(.cuppedTitle3)
                }
            }
            .liquidGlass()
            .cardPadding(Spacing.lg)
        }
        .padding()
    }
}

#Preview("Dark Mode") {
    VStack(spacing: Spacing.lg) {
        CuppedCard {
            VStack(alignment: .leading, spacing: Spacing.sm) {
                Text("Colombian Supremo")
                    .font(.cuppedTitle3)
                    .foregroundStyle(Color.cuppedInk)
                Text("Balanced and smooth with caramel notes.")
                    .font(.cuppedBody)
                    .foregroundStyle(Color.cuppedSecondary)
            }
        }
    }
    .padding()
    .background(Color.cuppedCanvas)
    .preferredColorScheme(.dark)
}
