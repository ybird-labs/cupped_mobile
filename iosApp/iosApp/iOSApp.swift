import Shared
import SwiftUI

@main
struct iOSApp: App {
    @State private var isBootstrapped = false
    @State private var authCoordinator = AuthCoordinator()
    @Environment(\.scenePhase) private var scenePhase

    private let serverHost: String

    init() {
        guard let baseUrl = Bundle.main.infoDictionary?["APIBaseURL"] as? String,
              !baseUrl.isEmpty else {
            fatalError(
                "APIBaseURL missing from Info.plist"
                + " - check Config.xcconfig"
            )
        }

        KoinHelper.shared.doInitKoin(baseUrl: baseUrl)
        self.serverHost = URL(string: baseUrl)?.host() ?? "localhost"
    }

    var body: some Scene {
        WindowGroup {
            ZStack(alignment: .top) {
                Group {
                    if isBootstrapped {
                        if authCoordinator.isAuthenticated {
                            MainTabView()
                                .environment(authCoordinator)
                        } else {
                            LoginView { bearerToken in
                                Task {
                                    _ = await authCoordinator.exchangeAndPersist(
                                        bearerToken: bearerToken
                                    )
                                }
                            }
                        }
                    } else {
                        Color.cuppedSurfaceApp
                            .ignoresSafeArea()
                    }
                }

                AuthFlowOverlay()
            }
            .environment(authCoordinator)
            .tint(.cuppedActionPrimary)
            .animation(.cuppedSpring, value: authCoordinator.authFlowStatus)
            .task {
                let restoredSession = await CookieStore.shared.restoreCookies(
                    to: WebViewConfiguration.cookieStore
                )

                CookieStore.shared.startObserving(
                    cookieStore: WebViewConfiguration.cookieStore,
                    targetHost: serverHost
                )

                if restoredSession {
                    authCoordinator.isAuthenticated = true
                }

                isBootstrapped = true
            }
            .onOpenURL { url in
                handleDeepLink(url)
            }
        }
        .onChange(of: scenePhase) { _, newPhase in
            if newPhase == .background {
                Task {
                    await CookieStore.shared.persistCookies(
                        from: WebViewConfiguration.cookieStore
                    )
                }
            }
        }
    }

    private func handleDeepLink(_ url: URL) {
        guard let components = URLComponents(
            url: url,
            resolvingAgainstBaseURL: false
        ) else { return }

        let validHosts: Set<String> = [
            serverHost.lowercased(),
            "cupped.cafe",
        ].filter { !$0.isEmpty }.reduce(into: []) {
            $0.insert($1.lowercased())
        }

        if components.scheme?.lowercased() == "https",
           let host = components.host?.lowercased(),
           validHosts.contains(host),
           components.path.hasPrefix("/users/log-in/") {
            let token = url.lastPathComponent
            guard !token.isEmpty, token != "log-in" else { return }

            Task {
                _ = await authCoordinator.handleMagicLinkToken(token)
            }
            return
        }

        if components.scheme?.lowercased() == "cupped",
           components.host == "auth",
           components.path == "/callback",
           let token = components.queryItems?
               .first(where: { $0.name == "token" })?
               .value,
           !token.isEmpty {
            Task {
                _ = await authCoordinator.handleMagicLinkToken(token)
            }
        }
    }
}
