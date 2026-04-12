import SwiftUI

/// Renders text with `@username` mentions highlighted as tappable spans.
struct MentionText: View {
    let text: String
    var onMentionTapped: ((String) -> Void)?

    var body: some View {
        Text(attributedText)
            .font(.cuppedSubheadline)
            .foregroundStyle(Color.cuppedInk)
            .environment(\.openURL, OpenURLAction { url in
                guard url.scheme == "mention",
                      let username = mentionName(from: url) else {
                    return .systemAction
                }

                onMentionTapped?(username)
                return .handled
            })
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
                  let attrRange = Range(swiftRange, in: result),
                  let usernameRange = Range(match.range(at: 1), in: text),
                  let mentionURL = mentionURL(for: String(text[usernameRange])) else { continue }

            result[attrRange].foregroundColor = .cuppedInfo
            result[attrRange].font = .cuppedText(size: 14, weight: .semibold)
            result[attrRange].link = mentionURL
        }

        return result
    }

    private func mentionURL(for username: String) -> URL? {
        var components = URLComponents()
        components.scheme = "mention"
        components.host = username
        return components.url
    }

    private func mentionName(from url: URL) -> String? {
        if let host = url.host, !host.isEmpty {
            return host
        }

        let prefix = "mention://"
        guard url.absoluteString.hasPrefix(prefix) else { return nil }
        return String(url.absoluteString.dropFirst(prefix.count))
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
