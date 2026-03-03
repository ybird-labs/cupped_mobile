// FeedView.swift
// Cupped - cafe.cupped.app
//
// The Feed tab's content view. Loads the Phoenix LiveView
// /feed page in a CuppedWebView. This is the primary
// proof-of-concept for the Phase 3 WebView infrastructure.
//
// Authentication flow:
//   In DEBUG builds, DevAuthView handles the mobile-session
//   exchange BEFORE this view renders, so makeUIView is a
//   simple one-line load. In Release builds, the WebView
//   loads /feed directly — if unauthenticated, Phoenix
//   redirects to the web login form.

import SwiftUI
import Shared
import WebKit

/// Displays the Phoenix LiveView `/feed` page in a
/// ``CuppedWebView``.
///
/// Overlays a progress bar during page loads. The WebView
/// handles all navigation internally (including
/// server-side redirects to the login page when
/// unauthenticated).
struct FeedView: View {
    /// Observable state bridged from the WKWebView via
    /// CuppedWebView.Coordinator's KVO observers.
    @StateObject private var webViewState = WebViewState()

    /// The Phoenix server base URL from Config.xcconfig.
    private let baseURL: String

    init() {
        self.baseURL = KoinHelper.shared.getBaseUrl()
    }

    var body: some View {
        ZStack {
            if let url = URL(
                string: "\(baseURL)/feed"
            ) {
                CuppedWebView(
                    url: url,
                    state: webViewState
                )
            }

            // Thin progress bar at the top edge, visible
            // only during page loads. Uses the design
            // system's primary color.
            if webViewState.isLoading {
                VStack {
                    ProgressView(
                        value: webViewState
                            .estimatedProgress
                    )
                    Spacer()
                }
            }
        }
        .background(Color.cuppedCanvas)
    }
}
