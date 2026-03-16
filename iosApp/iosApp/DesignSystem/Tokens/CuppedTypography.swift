import SwiftUI
import UIKit

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

extension Font {
    static let cuppedLargeTitle = makeLora(size: 48, weight: 700, textStyle: .largeTitle)
    static let cuppedTitle1 = makeLora(size: 36, weight: 600, textStyle: .title1)
    static let cuppedTitle2 = makeLora(size: 30, weight: 700, textStyle: .title2)
    static let cuppedTitle3 = makeLora(size: 24, weight: 400, textStyle: .title3)

    static let cuppedBodyLarge = Font.custom("Plus Jakarta Sans", size: 18, relativeTo: .body)
    static let cuppedBody = Font.custom("Plus Jakarta Sans", size: 16, relativeTo: .body)
    static let cuppedSubheadline = Font.custom("Plus Jakarta Sans", size: 14, relativeTo: .subheadline)
    static let cuppedCaption = Font.custom("Plus Jakarta Sans", size: 12, relativeTo: .caption)

    static func cuppedHeadline(
        size: CGFloat,
        weight: CGFloat = 700,
        textStyle: UIFont.TextStyle = .title1
    ) -> Font {
        makeLora(size: size, weight: weight, textStyle: textStyle)
    }

    static func cuppedText(
        size: CGFloat,
        weight: Font.Weight = .regular,
        relativeTo textStyle: Font.TextStyle = .body
    ) -> Font {
        Font.custom("Plus Jakarta Sans", size: size, relativeTo: textStyle)
            .weight(weight)
    }
}
