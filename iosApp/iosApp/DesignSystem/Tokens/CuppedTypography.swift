import SwiftUI
import UIKit

// MARK: - Lora Variable Font Helper

/// Lora is a variable font with a single axis: wght (400–700).
/// We construct it via Core Text so we can set the weight axis
/// explicitly and wrap with UIFontMetrics for Dynamic Type scaling.
private func makeLora(
    size: CGFloat,
    weight: CGFloat = 400,
    textStyle: UIFont.TextStyle = .body
) -> Font {
    let wghtTag = Int(UInt8(ascii: "w")) << 24
        | Int(UInt8(ascii: "g")) << 16
        | Int(UInt8(ascii: "h")) << 8
        | Int(UInt8(ascii: "t"))

    let descriptor = UIFontDescriptor(fontAttributes: [.family: "Lora"])
        .addingAttributes([
            UIFontDescriptor.AttributeName(
                rawValue: kCTFontVariationAttribute as String
            ): [wghtTag: weight]
        ])

    let baseFont = UIFont(descriptor: descriptor, size: size)
    let scaledFont = UIFontMetrics(forTextStyle: textStyle)
        .scaledFont(for: baseFont)
    return Font(scaledFont)
}

// MARK: - Typography Tokens

extension Font {
    // MARK: Headlines (Lora — variable serif)
    // Mapped from React / Design System: text-5xl / text-4xl / text-3xl / text-2xl

    /// 48pt Bold — hero titles, large display text (React: text-5xl font-bold)
    static let cuppedLargeTitle = makeLora(size: 48, weight: 700, textStyle: .largeTitle)

    /// 36pt SemiBold — primary headings (React: text-4xl font-semibold)
    static let cuppedTitle1 = makeLora(size: 36, weight: 600, textStyle: .title1)

    /// 30pt Bold — secondary headings (React: text-3xl font-bold)
    static let cuppedTitle2 = makeLora(size: 30, weight: 700, textStyle: .title2)

    /// 24pt Regular — tertiary headings (React: text-2xl)
    static let cuppedTitle3 = makeLora(size: 24, weight: 400, textStyle: .title3)

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

    /// Lora headline with proper variable font weight axis.
    /// Pass an explicit `textStyle` so Dynamic Type scales
    /// correctly; defaults to `.title` when omitted.
    static func cuppedHeadline(
        size: CGFloat,
        weight: CGFloat = 700,
        textStyle: UIFont.TextStyle = .title1
    ) -> Font {
        makeLora(size: size, weight: weight, textStyle: textStyle)
    }

    /// Plus Jakarta Sans body text with Dynamic Type support.
    static func cuppedText(
        size: CGFloat,
        weight: Font.Weight = .regular,
        relativeTo textStyle: Font.TextStyle = .body
    ) -> Font {
        Font.custom("Plus Jakarta Sans", size: size, relativeTo: textStyle)
            .weight(weight)
    }
}
