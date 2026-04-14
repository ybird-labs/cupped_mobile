import Foundation
import Observation

@MainActor
@Observable
final class AppLanguageStore {
    enum Option: String, CaseIterable, Identifiable {
        case system
        case english = "en"
        case spanish = "es"

        var id: String { rawValue }

        var locale: Locale? {
            switch self {
            case .system:
                nil
            case .english:
                Locale(identifier: "en")
            case .spanish:
                Locale(identifier: "es")
            }
        }
    }

    var selectedOption: Option {
        didSet {
            if selectedOption == .system {
                defaults.removeObject(forKey: storageKey)
            } else {
                defaults.set(selectedOption.rawValue, forKey: storageKey)
            }
        }
    }

    var locale: Locale {
        selectedOption.locale ?? .autoupdatingCurrent
    }

    private let defaults: UserDefaults
    private let storageKey = "selected_app_language"

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        if let storedValue = defaults.string(forKey: storageKey),
           let storedOption = Option(rawValue: storedValue) {
            selectedOption = storedOption
        } else {
            selectedOption = .system
        }
    }
}
