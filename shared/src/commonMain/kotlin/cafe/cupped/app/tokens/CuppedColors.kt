package cafe.cupped.app.tokens

object CuppedColors {
    // Surfaces
    val canvas = ColorToken(0xFFF8FAFCu)
    val card = ColorToken(0xFFFFFFFFu)

    // Surfaces — Borders
    val canvasBorder = ColorToken(0xFFE2E8F0u)
    val canvasBorderSubtle = ColorToken(0xFFF1F5F9u)

    // Text
    val ink = ColorToken(0xFF0F172Au)
    val secondary = ColorToken(0xFF475569u)
    val muted = ColorToken(0xFF94A3B8u)
    val inkInverse = ColorToken(0xFFF8FAFCu)

    // Brand
    val primary = ColorToken(0xFFE07A5Fu)
    val primaryHover = ColorToken(0xFFD16A4Fu)
    val primaryLight = ColorToken(0xFFFDF2F0u)
    val primaryMuted = ColorToken(0xFFE07A5Fu) // same hue as primary; Swift bridge applies 12% opacity

    // Feedback
    val success = ColorToken(0xFF22C55Eu)
    val error = ColorToken(0xFFEF4444u)
    val warning = ColorToken(0xFFF59E0Bu)
    val info = ColorToken(0xFF3B82F6u)

    // Feedback — Light Backgrounds
    val successLight = ColorToken(0xFFF0FDF4u)
    val errorLight = ColorToken(0xFFFEF2F2u)
    val warningLight = ColorToken(0xFFFFFBEBu)
    val infoLight = ColorToken(0xFFEFF6FFu)

    // Flavor Notes
    val fruity = ColorToken(0xFFE85D75u)
    val floral = ColorToken(0xFFA855F7u)
    val nutty = ColorToken(0xFFC9A66Bu)
    val chocolate = ColorToken(0xFF92603Du)
    val spice = ColorToken(0xFFB44D3Eu)
    val sweet = ColorToken(0xFFFBBF24u)
    val citrus = ColorToken(0xFFF4A259u)
    val green = ColorToken(0xFF6B8E50u)
    val berry = ColorToken(0xFFC23B5Eu)
    val roasted = ColorToken(0xFF4A3C31u)

    // Flavor Notes — Accessible Backgrounds (WCAG AA >=4.5:1 vs white)
    val fruityAccessible = ColorToken(0xFFC04055u)
    val floralAccessible = ColorToken(0xFF8B3FD4u)
    val citrusAccessible = ColorToken(0xFFC07B30u)
    val nuttyAccessible = ColorToken(0xFF8B7040u)
    val greenAccessible = ColorToken(0xFF557040u)

    // Gamification
    val xp = ColorToken(0xFFFBBF24u)
    val streak = ColorToken(0xFFF97316u)
    val badge = ColorToken(0xFFA855F7u)
}
