import SwiftUI

/// Renders text with `@username` mentions highlighted as tappable spans.
struct MentionText: View {
    let text: String
    var onMentionTapped: ((String) -> Void)?

    var body: some View {
        Text(attributedText)
            .font(.cuppedSubheadline)
            .foregroundStyle(Color.cuppedInk)
    }

    private var attributedText: AttributedString {
        var result = AttributedString(text)
        let nsText = text as NSString

        guard let regex = try? NSRegularExpression(pattern: "@(\\w+)") else {
            return result
        }

        let matches = regex.matches(
            in: text,
            range: NSRange(location: 0, length: nsText.length)
        )

        for match in matches {
            guard let swiftRange = Range(match.range, in: text),
                  let attrRange = Range(swiftRange, in: result) else { continue }

            result[attrRange].foregroundColor = .cuppedInfo
            result[attrRange].font = .cuppedText(size: 14, weight: .semibold)
        }

        return result
    }
}

#Preview("Mention Text") {
    VStack(alignment: .leading, spacing: Spacing.md) {
        MentionText(text: "Great extraction by @mike_barista today!")
        MentionText(text: "No mentions here, just a regular note.")
        MentionText(text: "@alice and @bob both loved this one.")
    }
    .padding()
}
