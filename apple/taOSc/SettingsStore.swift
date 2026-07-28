import Foundation
import Combine

final class SettingsStore: ObservableObject {
    @Published var serverURL: String {
        didSet {
            UserDefaults.standard.set(serverURL, forKey: "serverURL")
        }
    }

    @Published var showSettings = false

    init() {
        self.serverURL = UserDefaults.standard.string(forKey: "serverURL") ?? ""
    }

    var hasServerURL: Bool {
        !serverURL.isEmpty
    }

    func saveServerURL(_ url: String) {
        serverURL = url
    }
}
