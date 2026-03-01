import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        guard let baseUrl = Bundle.main.infoDictionary?["APIBaseURL"] as? String,
              !baseUrl.isEmpty else {
            fatalError("APIBaseURL missing from Info.plist – check Config.xcconfig")
        }
        KoinHelper.shared.doInitKoin(baseUrl: baseUrl)
    }

    var body: some Scene {
        WindowGroup {
            MainTabView()
        }
    }
}
