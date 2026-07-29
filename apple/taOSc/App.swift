import SwiftUI

@main
struct taOScApp: App {
    @StateObject private var settings = SettingsManager()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(settings)
                .onAppear {
                    settings.load()
                }
        }
    }
}
