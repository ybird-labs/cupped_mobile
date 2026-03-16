import SwiftUI

struct CuppedTextField: View {
    let placeholder: String
    @Binding var text: String

    var label: String? = nil
    var icon: String? = nil
    var error: String? = nil
    var isLoading: Bool = false
    var keyboardType: UIKeyboardType = .default
    var textContentType: UITextContentType? = nil
    var autocapitalization: TextInputAutocapitalization = .sentences
    var disableAutocorrection: Bool = false

    @FocusState private var isFocused: Bool

    private var hasError: Bool { error != nil }

    private var borderColor: Color {
        if hasError { return .cuppedInputBorderCritical }
        if isFocused { return .cuppedInputBorderFocused }
        return .cuppedInputBorder
    }

    private var borderWidth: CGFloat {
        (hasError || isFocused) ? 2 : 1
    }

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            labelView
            fieldContainer
            errorView
        }
        .animation(.easeInOut(duration: 0.2), value: error)
    }

    @ViewBuilder
    private var labelView: some View {
        if let label {
            Text(label)
                .font(.cuppedCaption)
                .fontWeight(.medium)
                .foregroundStyle(Color.cuppedTextSecondary)
                .tracking(1.2)
        }
    }

    private var fieldContainer: some View {
        HStack(spacing: Spacing.sm) {
            iconView
            placeholderTextField
        }
        .padding(.horizontal, Spacing.md)
        .padding(.vertical, Spacing.base)
        .background(Color.cuppedSurfaceCard)
        .clipShape(
            RoundedRectangle(
                cornerRadius: Radius.md,
                style: .continuous
            )
        )
        .overlay {
            RoundedRectangle(
                cornerRadius: Radius.md,
                style: .continuous
            )
            .strokeBorder(borderColor, lineWidth: borderWidth)
        }
        .shadow(
            color: isFocused
                ? Color.cuppedActionPrimary.opacity(0.2)
                : .clear,
            radius: isFocused ? 4 : 0,
            x: 0, y: 0
        )
        .animation(.easeInOut(duration: 0.15), value: isFocused)
        .opacity(isLoading ? 0.5 : 1)
    }

    @ViewBuilder
    private var iconView: some View {
        if let icon {
            Image(systemName: icon)
                .foregroundStyle(Color.cuppedInputCursor)
                .font(.system(size: 16))
        }
    }

    private var placeholderTextField: some View {
        ZStack(alignment: .leading) {
            if text.isEmpty {
                Text(placeholder)
                    .font(.cuppedBody)
                    .foregroundStyle(Color.cuppedInputPlaceholderBase.opacity(0.5))
                    .allowsHitTesting(false)
            }
            TextField("", text: $text)
                .font(.cuppedBody)
                .foregroundStyle(Color.cuppedTextPrimary)
                .keyboardType(keyboardType)
                .textContentType(textContentType)
                .textInputAutocapitalization(autocapitalization)
                .autocorrectionDisabled(disableAutocorrection)
                .accessibilityLabel(label ?? placeholder)
                .focused($isFocused)
                .disabled(isLoading)
        }
        .tint(Color.cuppedInputCursor)
    }

    @ViewBuilder
    private var errorView: some View {
        if let error {
            Text(error)
                .font(.cuppedSubheadline)
                .foregroundStyle(Color.cuppedStatusErrorForeground)
                .frame(maxWidth: .infinity, alignment: .leading)
                .transition(
                    .opacity.combined(
                        with: .offset(y: -5)
                    )
                )
        }
    }
}
