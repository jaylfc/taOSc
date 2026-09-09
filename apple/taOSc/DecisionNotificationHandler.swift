import Foundation
import UserNotifications

enum DecisionCategory: String {
    case approveDeny = "DECISION_APPROVE_DENY"
    case freeText = "DECISION_FREE_TEXT"
    case options = "DECISION_OPTIONS"
}

enum DecisionAction: String {
    case approve = "approve"
    case reject = "reject"
    case addNote = "add_note"
    case quickReply = "quick_reply"
}

final class DecisionNotificationHandler: NSObject, UNUserNotificationCenterDelegate {
    static let shared = DecisionNotificationHandler()

    override init() {
        super.init()
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                willPresent notification: UNNotification,
                                withCompletionHandler completionHandler:
                                @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.banner, .sound])
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                didReceive response: UNNotificationResponse,
                                withCompletionHandler completionHandler: @escaping () -> Void) {
        let userInfo = response.notification.request.content.userInfo
        let actionIdentifier = response.actionIdentifier
        let decisionId = userInfo["decision_id"] as? String ?? ""
        let decisionType = userInfo["decision_type"] as? String ?? ""
        let source = decisionType

        if actionIdentifier == UNNotificationDefaultActionIdentifier {
            handleTapAction(userInfo: userInfo, decisionId: decisionId, decisionType: decisionType, source: source)
        } else if let actionId = actionIdentifier,
                  actionId != UNNotificationDismissActionIdentifier {
            handleActionTap(actionId, userInfo: userInfo, decisionId: decisionId, decisionType: decisionType, source: source)
        }

        completionHandler()
    }

    private func handleTapAction(userInfo: [String: Any],
                                  decisionId: String,
                                  decisionType: String,
                                  source: String) {
        NotificationCenter.default.post(
            name: .decisionTapped,
            object: nil,
            userInfo: ["decisionId": decisionId,
                       "decisionType": decisionType,
                       "source": source,
                       "url": userInfo["url"]]
        )
    }

private func handleActionTap(_ actionId: String,
                              userInfo: [String: Any],
                              decisionId: String,
                              decisionType: String,
                              source: String) {
        var otherValue: String?
        if actionId == DecisionAction.addNote.rawValue ||
            actionId == DecisionAction.quickReply.rawValue {
            otherValue = userInfo["typed_text"] as? String
        }

        let body: [String: Any] = [
            "value": actionId,
            "other_value": otherValue ?? "",
            "source": source
        ]

        Task {
            await sendAnswer(decisionId: decisionId, body: body)
        }
    }

    private func sendAnswer(decisionId: String, body: [String: Any]) async {
        do {
            let bearer = try KeychainStore.shared.readDeviceId()
            let url = URL(string: "/api/decisions/\(decisionId)/answer")!

            var request = URLRequest(url: url)
            request.httpMethod = "POST"
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.setValue("Bearer \(bearer)", forHTTPHeaderField: "Authorization")
            request.httpBody = try JSONSerialization.data(withJSONObject: body)

            let (_, response) = try await URLSession.shared.data(for: request)
            let httpResponse = response as? HTTPURLResponse

            switch httpResponse?.statusCode {
            case 401:
                NotificationCenter.default.post(name: .openPairingScreen, object: nil)
            case 404, 409:
                postAlreadyAnsweredNotification()
            default: break
            }
        } catch {
            // 401 opens pairing screen on auth failure
        }
    }

    private func postAlreadyAnsweredNotification() {
        let content = UNMutableNotificationContent()
        content.title = "Already answered"
        content.body = "This decision has already been answered."
        content.sound = .default

        let request = UNNotificationRequest(identifier: "already_answered",
                                           content: content,
                                           trigger: nil)
        UNUserNotificationCenter.current().add(request) { error in
            if let error = error {
                print("Error posting already answered: \(error)")
            }
        }
    }
}

extension Notification.Name {
    static let decisionTapped = Notification.Name("decisionTapped")
    static let openPairingScreen = Notification.Name("openPairingScreen")
    static let alreadyAnswered = Notification.Name("alreadyAnswered")
}