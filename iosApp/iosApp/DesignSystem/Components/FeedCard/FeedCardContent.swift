import SwiftUI

struct FeedCardContent: View {
    let coffee: CoffeeInfo
    let flavors: [FlavorNote]
    let notes: String?
    let recipe: RecipeInfo?
    var onBaristaTapped: ((String) -> Void)?

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            // Coffee name
            Text(coffee.name)
                .font(.cuppedHeadline(size: 20, weight: 700, textStyle: .headline))
                .foregroundStyle(Color.cuppedInk)
                .lineLimit(2)

            // Roaster + Barista
            if coffee.roaster != nil || coffee.barista != nil {
                roasterBaristaRow
            }

            // Farm + Origin
            if coffee.farm != nil || coffee.origin != nil {
                farmOriginRow
            }

            // Flavor tags
            if !flavors.isEmpty {
                FlowLayout(spacing: Spacing.xs) {
                    ForEach(flavors, id: \.self) { flavor in
                        FlavorTag(flavor: flavor)
                    }
                }
                .padding(.top, Spacing.xs)
            }

            // Notes
            if let notes, !notes.isEmpty {
                MentionText(text: notes, onMentionTapped: onBaristaTapped)
                    .padding(.top, Spacing.xs)
            }

            // Recipe
            if let recipe, hasRecipeData(recipe) {
                recipeRow(recipe)
                    .padding(.top, Spacing.xs)
            }
        }
    }

    // MARK: - Roaster + Barista

    private var roasterBaristaRow: some View {
        HStack(spacing: Spacing.sm) {
            if let roaster = coffee.roaster {
                HStack(spacing: Spacing.xs) {
                    Image(systemName: "shippingbox")
                        .font(.system(size: 12))
                    Text(roaster)
                        .font(.cuppedSubheadline)
                        .fontWeight(.bold)
                }
                .foregroundStyle(Color.cuppedPrimary)
            }

            if coffee.roaster != nil && coffee.barista != nil {
                Text("\u{2022}")
                    .font(.cuppedCaption)
                    .foregroundStyle(Color.cuppedMuted.opacity(0.5))
            }

            if let barista = coffee.barista {
                HStack(spacing: Spacing.xs) {
                    Image(systemName: "person.fill")
                        .font(.system(size: 10))
                        .foregroundStyle(Color.cuppedMuted)

                    Button {
                        onBaristaTapped?(barista)
                    } label: {
                        Text("@\(barista)")
                            .font(.cuppedSubheadline)
                            .fontWeight(.medium)
                            .foregroundStyle(Color.cuppedInfo)
                    }
                    .buttonStyle(TapScaleButtonStyle())
                }
            }
        }
    }

    // MARK: - Farm + Origin

    private var farmOriginRow: some View {
        HStack(spacing: Spacing.xs) {
            if let farm = coffee.farm {
                Text(farm)
            }
            if coffee.farm != nil && coffee.origin != nil {
                Text("\u{2022}")
                    .foregroundStyle(Color.cuppedMuted.opacity(0.5))
            }
            if let origin = coffee.origin {
                Text(origin)
            }
        }
        .font(.cuppedSubheadline)
        .foregroundStyle(Color.cuppedMuted)
    }

    // MARK: - Recipe

    private func hasRecipeData(_ recipe: RecipeInfo) -> Bool {
        recipe.ratio != nil || recipe.waterTemp != nil || recipe.grindSize != nil
    }

    private func recipeRow(_ recipe: RecipeInfo) -> some View {
        HStack(spacing: Spacing.md) {
            if let ratio = recipe.ratio {
                recipeItem(icon: "drop", text: ratio)
            }
            if let temp = recipe.waterTemp {
                recipeItem(icon: "thermometer.medium", text: temp)
            }
            if let grind = recipe.grindSize {
                HStack(spacing: Spacing.xs) {
                    Text("Grind:")
                        .fontWeight(.medium)
                    Text(grind)
                }
                .font(.cuppedCaption)
                .foregroundStyle(Color.cuppedMuted)
            }
        }
    }

    private func recipeItem(icon: String, text: String) -> some View {
        HStack(spacing: Spacing.xs) {
            Image(systemName: icon)
                .font(.system(size: 12))
            Text(text)
        }
        .font(.cuppedCaption)
        .foregroundStyle(Color.cuppedMuted)
    }
}

#Preview("Feed Card Content") {
    FeedCardContent(
        coffee: CoffeeInfo(
            name: "Ethiopian Yirgacheffe",
            roaster: "Counter Culture",
            barista: "mike_barista",
            farm: "Konga Cooperative",
            origin: "Yirgacheffe, Ethiopia"
        ),
        flavors: [.floral, .citrus],
        notes: "Absolutely stunning cup. @mike_barista nailed the extraction perfectly.",
        recipe: RecipeInfo(ratio: "1:16", waterTemp: "205\u{00B0}F", grindSize: "45")
    )
    .padding()
    .background(Color.cuppedCard)
}

#Preview("Content - Minimal") {
    FeedCardContent(
        coffee: CoffeeInfo(
            name: "House Blend",
            roaster: nil,
            barista: nil,
            farm: nil,
            origin: nil
        ),
        flavors: [],
        notes: nil,
        recipe: nil
    )
    .padding()
    .background(Color.cuppedCard)
}
