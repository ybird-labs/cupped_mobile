// DevAuthView.swift
// Cupped - cafe.cupped.app
//
// DEBUG-only screen for testing the mobile-session
// exchange flow during development. Allows pasting a
// bearer token obtained from the Phoenix console or
// POST /api/v1/auth/verify.
//
// Gate logic (in iOSApp.swift):
//   - Shown on first launch when no persisted cookies
//     exist in Keychain.
//   - Skipped automatically when a previous session was
//     persisted (CookieStore.shared.hasPersistedCookies()).
//   - "Skip (use web login)" bypasses to MainTabView,
//     where the WebView will redirect to Phoenix's web
//     login form.
//
// This file is NOT compiled in Release builds.

#if DEBUG
import SwiftUI
import Shared

/// Dev-only screen for exchanging a bearer token for a
/// session cookie via ``MobileSessionClient``.
///
/// Calls ``MobileSessionClient/exchangeToken(_:baseURL:redirectPath:)``
/// directly and displays real success/failure feedback.
/// No `UserDefaults` token passing — the exchange happens
/// inline with proper async/await.
struct DevAuthView: View {
    /// Set to `true` to dismiss this screen and show
    /// `MainTabView`.
    @Binding var isAuthenticated: Bool

    /// The raw bearer token pasted by the developer.
    @State private var bearerToken = ""

    /// Error message from the last failed exchange, or
    /// `nil` if no error or not yet attempted.
    @State private var errorMessage: String?

    /// Whether an exchange is currently in progress
    /// (drives the loading spinner on the button).
    @State private var isExchanging = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                Text("Dev Authentication")
                    .font(.cuppedTitle2)

                Text(
                    "Paste a bearer token obtained from "
                    + "the Phoenix console or "
                    + "POST /api/v1/auth/verify to "
                    + "establish a WebView session."
                )
                .font(.cuppedBody)
                .foregroundStyle(Color.cuppedMuted)
                .multilineTextAlignment(.center)

                TextField(
                    "Bearer token",
                    text: $bearerToken,
                    axis: .vertical
                )
                .textFieldStyle(.roundedBorder)
                .font(.system(
                    .caption, design: .monospaced
                ))
                .lineLimit(3...6)

                if let error = errorMessage {
                    Text(error)
                        .font(.cuppedCaption)
                        .foregroundStyle(
                            Color.cuppedError
                        )
                }

                CuppedButton(
                    title: "Exchange for Session",
                    style: .primary,
                    isLoading: isExchanging
                ) {
                    Task {
                        await exchangeToken()
                    }
                }
                .disabled(
                    bearerToken.trimmingCharacters(
                        in: .whitespacesAndNewlines
                    ).isEmpty || isExchanging
                )

                Button("Skip (use web login)") {
                    isAuthenticated = true
                }
                .font(.cuppedBody)
                .foregroundStyle(Color.cuppedMuted)
            }
            .padding()
            .background(Color.cuppedCanvas)
            .navigationTitle("Dev Auth")
        }
    }

    private func exchangeToken() async {
        isExchanging = true
        errorMessage = nil

        let token = bearerToken.trimmingCharacters(
            in: .whitespacesAndNewlines
        )
        let client = MobileSessionClient()
        let baseURL = KoinHelper.shared.getBaseUrl()
        let result = await client.exchangeToken(
            token,
            baseURL: baseURL
        )

        isExchanging = false

        switch result {
        case .success:
            // Persist cookies immediately so
            // DevAuthView is skipped on next launch
            await CookieStore.shared.persistCookies(
                from:
                    WebViewConfiguration.cookieStore
            )
            isAuthenticated = true
        case .failure(let reason):
            errorMessage =
                "Exchange failed: \(reason)"
        }
    }
}
#endif
