import SwiftUI
import UIKit
import CoreText

// MARK: - Fraunces Variable Font Helper

/// Fraunces has three variable axes: wght, opsz, SOFT.
/// WONK was pinned to 0 at build time via fonttools instancer (clean glyphs baked in).
/// iOS does NOT auto-set opsz to match font-size, so we set axes explicitly via Core Text.
private enum FrauncesFontAxis {
    // OpenType axis tags encoded as Int (4-byte ASCII tag)
    static let wght = tag("wght") // weight (100–900)
    static let opsz = tag("opsz") // optical size (9–144)
    static let soft = tag("SOFT") // softness (0=sharp, 100=round)

    private static func tag(_ s: String) -> Int {
        let bytes = Array(s.utf8)
        return Int(bytes[0]) << 24 | Int(bytes[1]) << 16 | Int(bytes[2]) << 8 | Int(bytes[3])
    }
}

private func makeFraunces(
    size: CGFloat,
    weight: CGFloat = 400,
    opticalSize: CGFloat? = nil,
    soft: CGFloat = 0,
    textStyle: UIFont.TextStyle = .body
) -> Font {
    let variations: [Int: CGFloat] = [
        FrauncesFontAxis.wght: weight,
        FrauncesFontAxis.opsz: opticalSize ?? size,
        FrauncesFontAxis.soft: soft,
    ]

    let descriptor = UIFontDescriptor(fontAttributes: [.family: "Fraunces"])
        .addingAttributes([
            UIFontDescriptor.AttributeName(rawValue: kCTFontVariationAttribute as String): variations
        ])

    let baseFont = UIFont(descriptor: descriptor, size: size)
    let scaledFont = UIFontMetrics(forTextStyle: textStyle).scaledFont(for: baseFont)
    return Font(scaledFont)
}

// MARK: - Typography Tokens

extension Font {
    // MARK: Headlines (Fraunces — variable serif)
    // Mapped from React: text-5xl / text-4xl / text-3xl / text-2xl

    /// 48pt Bold — hero titles, large display text (React: text-5xl font-bold)
    static let cuppedLargeTitle = makeFraunces(size: 48, weight: 700, textStyle: .largeTitle)

    /// 36pt SemiBold — primary headings (React: text-4xl font-semibold)
    static let cuppedTitle1 = makeFraunces(size: 36, weight: 600, textStyle: .title1)

    /// 30pt Medium — secondary headings (React: text-3xl font-medium)
    static let cuppedTitle2 = makeFraunces(size: 30, weight: 500, textStyle: .title2)

    /// 24pt Regular — tertiary headings (React: text-2xl)
    static let cuppedTitle3 = makeFraunces(size: 24, weight: 400, textStyle: .title3)

    // MARK: Body (Plus Jakarta Sans — variable sans-serif)
    // Mapped from React: text-lg / text-base / text-sm / text-xs

    /// 18pt Regular — large body, descriptions (React: text-lg leading-relaxed)
    static let cuppedBodyLarge = Font.custom("Plus Jakarta Sans", size: 18, relativeTo: .body)

    /// 16pt Regular — standard body text (React: text-base)
    static let cuppedBody = Font.custom("Plus Jakarta Sans", size: 16, relativeTo: .body)

    /// 14pt Regular — secondary text, small UI (React: text-sm)
    static let cuppedSubheadline = Font.custom("Plus Jakarta Sans", size: 14, relativeTo: .subheadline)

    /// 12pt Regular — captions, tags, labels (React: text-xs)
    static let cuppedCaption = Font.custom("Plus Jakarta Sans", size: 12, relativeTo: .caption)

    // MARK: Weight Variants

    /// Fraunces headline with proper variable font axes (opsz auto-matches size)
    static func cuppedHeadline(size: CGFloat, weight: CGFloat = 700) -> Font {
        makeFraunces(size: size, weight: weight)
    }

    /// Plus Jakarta Sans body text
    static func cuppedText(size: CGFloat, weight: Font.Weight = .regular) -> Font {
        Font.custom("Plus Jakarta Sans", size: size).weight(weight)
    }
}
