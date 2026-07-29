import Foundation
import Combine

final class SettingsManager: ObservableObject {
    @Published var serverURL: String = ""
    @Published var hasServerURL: Bool = false

    private let userDefaultsKey = "taOScServerURL"
    private let defaults: UserDefaults

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    func load() {
        let saved = defaults.string(forKey: userDefaultsKey) ?? ""
        serverURL = saved
        hasServerURL = !saved.isEmpty
    }

    func save(url: String) {
        let normalized = URLValidator.normalizeURL(url)
        serverURL = normalized
        defaults.set(normalized, forKey: userDefaultsKey)
        hasServerURL = true
    }

    func clear() {
        serverURL = ""
        defaults.removeObject(forKey: userDefaultsKey)
        hasServerURL = false
    }
}
