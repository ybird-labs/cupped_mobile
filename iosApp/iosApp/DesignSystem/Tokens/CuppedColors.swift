import SwiftUI
import Shared

// MARK: - Bridge from KMP ColorToken to SwiftUI Color

extension Color {
    init(_ token: ColorToken) {
        self.init(
            red: Double(token.red),
            green: Double(token.green),
            blue: Double(token.blue),
            opacity: Double(token.alpha)
        )
    }
}

// MARK: - Surfaces

extension Color {
    static let cuppedCanvas = Color(CuppedColors.shared.canvas)
    static let cuppedCard = Color(CuppedColors.shared.card)
    static let cuppedSurfaceApp = Color(CuppedColors.shared.canvas)
    static let cuppedSurfaceCard = Color(CuppedColors.shared.card)
}

// MARK: - Surface Borders

extension Color {
    static let cuppedCanvasBorder = Color(CuppedColors.shared.canvasBorder)
    static let cuppedCanvasBorderSubtle = Color(CuppedColors.shared.canvasBorderSubtle)
    static let cuppedBorderDefault = Color(CuppedColors.shared.canvasBorder)
}

// MARK: - Text

extension Color {
    static let cuppedInk = Color(CuppedColors.shared.ink)
    static let cuppedSecondary = Color(CuppedColors.shared.secondary)
    static let cuppedMuted = Color(CuppedColors.shared.muted)
    static let cuppedInkInverse = Color(CuppedColors.shared.inkInverse)
    static let cuppedTextPrimary = Color(CuppedColors.shared.ink)
    static let cuppedTextSecondary = Color(CuppedColors.shared.secondary)
    static let cuppedTextMuted = Color(CuppedColors.shared.muted)
}

// MARK: - Brand

extension Color {
    static let cuppedPrimary = Color(CuppedColors.shared.primary)
    static let cuppedPrimaryHover = Color(CuppedColors.shared.primaryHover)
    static let cuppedPrimaryLight = Color(CuppedColors.shared.primaryLight)
    static let cuppedPrimaryMuted = Color(CuppedColors.shared.primaryMuted).opacity(0.12)
    static let cuppedActionPrimary = Color(CuppedColors.shared.primary)
}

// MARK: - Feedback

extension Color {
    static let cuppedSuccess = Color(CuppedColors.shared.success)
    static let cuppedError = Color(CuppedColors.shared.error)
    static let cuppedWarning = Color(CuppedColors.shared.warning)
    static let cuppedInfo = Color(CuppedColors.shared.info)
    static let cuppedStatusErrorForeground = Color(CuppedColors.shared.error)
}

// MARK: - Input Fields

extension Color {
    static let cuppedInputTint = Color(CuppedColors.shared.muted)
    static let cuppedInputPlaceholder = Color(CuppedColors.shared.muted)
    static let cuppedInputBorder = Color(CuppedColors.shared.canvasBorder)
    static let cuppedInputBorderFocused = Color(CuppedColors.shared.primary)
    static let cuppedInputBorderCritical = Color(CuppedColors.shared.error)
    static let cuppedInputCursor = Color(CuppedColors.shared.muted)
    static let cuppedInputPlaceholderBase = Color(CuppedColors.shared.muted)
}

// MARK: - Feedback Light Backgrounds

extension Color {
    static let cuppedSuccessLight = Color(CuppedColors.shared.successLight)
    static let cuppedErrorLight = Color(CuppedColors.shared.errorLight)
    static let cuppedWarningLight = Color(CuppedColors.shared.warningLight)
    static let cuppedInfoLight = Color(CuppedColors.shared.infoLight)
}

// MARK: - Flavor Notes

extension Color {
    static let cuppedFruity = Color(CuppedColors.shared.fruity)
    static let cuppedFloral = Color(CuppedColors.shared.floral)
    static let cuppedNutty = Color(CuppedColors.shared.nutty)
    static let cuppedChocolate = Color(CuppedColors.shared.chocolate)
    static let cuppedSpice = Color(CuppedColors.shared.spice)
    static let cuppedSweet = Color(CuppedColors.shared.sweet)
    static let cuppedCitrus = Color(CuppedColors.shared.citrus)
    static let cuppedGreen = Color(CuppedColors.shared.green)
    static let cuppedBerry = Color(CuppedColors.shared.berry)
    static let cuppedRoasted = Color(CuppedColors.shared.roasted)
}

// MARK: - Flavor Notes — Accessible Backgrounds (WCAG AA)

extension Color {
    static let cuppedFruityAccessible = Color(CuppedColors.shared.fruityAccessible)
    static let cuppedFloralAccessible = Color(CuppedColors.shared.floralAccessible)
    static let cuppedCitrusAccessible = Color(CuppedColors.shared.citrusAccessible)
    static let cuppedNuttyAccessible = Color(CuppedColors.shared.nuttyAccessible)
    static let cuppedGreenAccessible = Color(CuppedColors.shared.greenAccessible)
}

// MARK: - Gamification

extension Color {
    static let cuppedXP = Color(CuppedColors.shared.xp)
    static let cuppedStreak = Color(CuppedColors.shared.streak)
    static let cuppedBadge = Color(CuppedColors.shared.badge)
}
