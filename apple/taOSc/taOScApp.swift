import SwiftUI

@main
struct taOScApp: App {
    @StateObject private var settings = SettingsStore()
    @StateObject private var errorState = ErrorStateStore()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(settings)
                .environmentObject(errorState)
        }
    }
}
