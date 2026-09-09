import SwiftUI

@main
struct taOScApp: App {
    @StateObject private var settings = SettingsStore()
    @StateObject private var errorState = ErrorStateStore()
    @State private var categoriesRegistered = false

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(settings)
                .environmentObject(errorState)
        }
        .onAppear {
            // 1. Register notification categories on first launch
            if !categoriesRegistered {
                UNUserNotificationCenter.current().delegate = DecisionNotificationHandler.shared
                UNUserNotificationCenter.current().setNotificationCategories([
                    UNNotificationCategory(
                        identifier: "DECISION_APPROVE_DENY",
                        actions: [
                            UNNotificationAction(identifier: "approve", title: "Approve", options: .isTextInputAllowed),
                            UNNotificationAction(identifier: "reject", title: "Reject", options: []),
                            UNNotificationAction(identifier: "add_note", title: "Add Note", options: .isTextInputAllowed)
                        ],
                        intentIdentifiers: [],
                        options: [.customDismissAction]
                    ),
                    UNNotificationCategory(
                        identifier: "DECISION_FREE_TEXT",
                        actions: [
                            UNNotificationAction(identifier: "quick_reply", title: "Reply", options: .isTextInputAllowed)
                        ],
                        intentIdentifiers: [],
                        options: [.customDismissAction]
                    ),
                    UNNotificationCategory(
                        identifier: "DECISION_OPTIONS",
                        actions: [],  // v1: NO custom actions; options rendered from userInfo["options"] by tapping through into the app
                        intentIdentifiers: [],
                        options: [.customDismissAction]
                    )
                ])
                categoriesRegistered = true
            }

            // 2. After pairing succeeds, register for push notifications
            if settings.isPaired && settings.hasServerURL {
                Task {
                    await PushRegistrar.shared.registerIfPaired(
                        baseURL: URL(string: settings.serverURL)!)
                }
            }
        }
    }
}

extension taOScApp {
    func application(_ application: UIApplication,
                     didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        PushRegistrar.shared.setDeviceToken(deviceToken)
    }

    func application(_ application: UIApplication,
                     didFailToRegisterForRemoteNotificationsWithError error: Error) {
        // Failure shows one non-blocking status line in Settings, never an alert
    }
}