// WebViewState.swift
// Cupped - cafe.cupped.app
//
// Observable state object bridging WKWebView's KVO
// properties to SwiftUI. Owned by the hosting SwiftUI
// view (e.g., FeedView) and passed into CuppedWebView,
// whose Coordinator updates these properties via KVO
// observers on the underlying WKWebView.

import Foundation
import Combine

/// Observable bridge between WKWebView's internal state
/// and SwiftUI views.
///
/// `CuppedWebView.Coordinator` observes the underlying
/// `WKWebView` via KVO and forwards changes to these
/// published properties. SwiftUI views can then react
/// to loading progress, navigation state, and errors
/// without holding a direct reference to the web view.
///
/// ## Usage
/// ```swift
/// @StateObject var state = WebViewState()
/// // ...
/// CuppedWebView(url: feedURL, state: state)
/// if state.isLoading {
///     ProgressView(value: state.estimatedProgress)
/// }
/// ```
final class WebViewState: ObservableObject {
    /// Whether the web view is currently loading content.
    @Published var isLoading = false

    /// Page load progress in the range `0.0...1.0`.
    @Published var estimatedProgress: Double = 0

    /// Whether the web view can navigate backward.
    @Published var canGoBack = false

    /// Whether the web view can navigate forward.
    @Published var canGoForward = false

    /// The URL currently displayed by the web view, or
    /// `nil` before the first navigation commits.
    @Published var currentURL: URL?

    /// The most recent navigation error, or `nil` if the
    /// last navigation succeeded.
    @Published var error: Error?
}
