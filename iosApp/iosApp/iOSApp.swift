import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        KoinHelper.shared.doInitKoin(baseUrl: "http://localhost:4000")
    }

    var body: some Scene {
        WindowGroup {
            MainTabView()
        }
    }
}
