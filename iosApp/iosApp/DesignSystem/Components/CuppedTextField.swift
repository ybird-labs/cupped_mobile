import SwiftUI

/// A design-system text field with built-in label, icon, placeholder,
/// border states, focus ring, and inline error display.
///
/// Uses the ZStack-overlay placeholder pattern so placeholder styling
/// is fully controlled because SwiftUI's `prompt:` does not give us the
/// placeholder styling control this component needs.
///
/// ## Usage
/// ```swift
/// CuppedTextField(
///     label: "EMAIL ADDRESS",
///     placeholder: "your@email.com",
///     text: $email,
///     icon: "envelope",
///     error: errorMessage,
///     isLoading: isLoading,
///     keyboardType: .emailAddress,
///     textContentType: .emailAddress,
///     autocapitalization: .never,
///     disableAutocorrection: true
/// )
/// ```
struct CuppedTextField: View {
    // MARK: - Required

    let placeholder: String
    @Binding var text: String

    // MARK: - Optional

    var label: String? = nil
    var icon: String? = nil
    var error: String? = nil
    var isLoading: Bool = false
    var keyboardType: UIKeyboardType = .default
    var textContentType: UITextContentType? = nil
    var autocapitalization: TextInputAutocapitalization = .sentences
    var disableAutocorrection: Bool = false

    // MARK: - Internal State

    @FocusState private var isFocused: Bool

    // MARK: - Computed

    private var hasError: Bool { error != nil }

    private var borderColor: Color {
        if hasError { return .cuppedInputBorderCritical }
        if isFocused { return .cuppedInputBorderFocused }
        return .cuppedInputBorder
    }

    private var borderWidth: CGFloat {
        (hasError || isFocused) ? 2 : 1
    }

    // MARK: - Body

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            labelView
            fieldContainer
            errorView
        }
        .animation(.easeInOut(duration: 0.2), value: error)
    }

    // MARK: - Label

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

    // MARK: - Field Container

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

    // MARK: - Icon

    @ViewBuilder
    private var iconView: some View {
        if let icon {
            Image(systemName: icon)
                .foregroundStyle(Color.cuppedInputCursor)
                .font(.system(size: 16))
        }
    }

    // MARK: - Placeholder + TextField (ZStack overlay)

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

    // MARK: - Error

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

// MARK: - Previews

#Preview("Default") {
    VStack(spacing: Spacing.lg) {
        CuppedTextField(
            placeholder: "your@email.com",
            text: .constant(""),
            label: "EMAIL ADDRESS",
            icon: "envelope"
        )

        CuppedTextField(
            placeholder: "your@email.com",
            text: .constant("jean@cupped.cafe"),
            label: "EMAIL ADDRESS",
            icon: "envelope"
        )

        CuppedTextField(
            placeholder: "your@email.com",
            text: .constant("bad-email"),
            label: "EMAIL ADDRESS",
            icon: "envelope",
            error: "Please enter a valid email address"
        )

        CuppedTextField(
            placeholder: "your@email.com",
            text: .constant(""),
            label: "EMAIL ADDRESS",
            icon: "envelope",
            isLoading: true
        )
    }
    .padding(Spacing.lg)
    .background(Color.cuppedSurfaceApp)
}
