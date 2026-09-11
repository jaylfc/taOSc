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
    static var baseURL: URL?
    private(set) var urlSession: URLSession = .shared

    override init() {
        super.init()
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                willPresent notification: UNNotification,
                                withCompletionHandler completionHandler:
                                @escaping (UNNotificationPresentationOptions) -> Void) {
        registerCategories(from: notification.request.content.userInfo)
        completionHandler([.banner, .sound])
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                didReceive response: UNNotificationResponse,
                                withCompletionHandler completionHandler: @escaping () -> Void) {
        let userInfo = response.notification.request.content.userInfo
        let actionIdentifier = response.actionIdentifier
        let decisionId = userInfo["decision_id"] as? String ?? ""
        let decisionType = userInfo["decision_type"] as? String ?? ""

        registerCategories(from: userInfo)

        if actionIdentifier == UNNotificationDefaultActionIdentifier {
            handleTapAction(userInfo: userInfo, decisionId: decisionId, decisionType: decisionType)
        } else if actionIdentifier != UNNotificationDismissActionIdentifier {
            handleActionTap(actionIdentifier, userInfo: userInfo, decisionId: decisionId, decisionType: decisionType, response: response)
        }

        completionHandler()
    }

    func registerCategories(from userInfo: [String: Any]) {
        let actionsPayload = userInfo["actions"] as? [[String: Any]] ?? []
        let optionsPayload = userInfo["options"] as? [String] ?? []
        let decisionType = userInfo["decision_type"] as? String ?? ""

        var actions: [UNNotificationAction] = []

        if actionsPayload.isEmpty && !optionsPayload.isEmpty {
            for option in optionsPayload {
                actions.append(UNNotificationAction(identifier: option, title: option, options: []))
            }
        } else {
            for actionDict in actionsPayload {
                let id = actionDict["id"] as? String ?? ""
                let title = actionDict["title"] as? String ?? id
                let requiresText = actionDict["requires_text"] as? Bool ?? false
                let options: UNNotificationAction.Options = requiresText ? .isTextInputAllowed : []
                actions.append(UNNotificationAction(identifier: id, title: title, options: options))
            }
        }

        let categoryIdentifier: String
        switch decisionType {
        case "approve_deny": categoryIdentifier = DecisionCategory.approveDeny.rawValue
        case "free_text": categoryIdentifier = DecisionCategory.freeText.rawValue
        case "single_select", "multi_select": categoryIdentifier = DecisionCategory.options.rawValue
        default: return
        }

        let category = UNNotificationCategory(identifier: categoryIdentifier, actions: actions, intentIdentifiers: [], options: [.customDismissAction])
        UNUserNotificationCenter.current().setNotificationCategories([category])
    }

    func handleTapAction(userInfo: [String: Any],
                                  decisionId: String,
                                  decisionType: String) {
        let options = userInfo["options"] as? [String] ?? []
        NotificationCenter.default.post(
            name: .decisionTapped,
            object: nil,
            userInfo: ["decisionId": decisionId,
                       "decisionType": decisionType,
                       "url": userInfo["url"],
                       "options": options]
        )
    }

    func handleActionTap(_ actionId: String,
                                 userInfo: [String: Any],
                                 decisionId: String,
                                 decisionType: String,
                                 response: UNNotificationResponse) {
        var otherValue: String?
        if actionId == DecisionAction.addNote.rawValue ||
            actionId == DecisionAction.quickReply.rawValue {
            if let textResponse = response as? UNTextInputNotificationResponse {
                otherValue = textResponse.userText
            } else {
                otherValue = userInfo["typed_text"] as? String
            }
        }

        let body = buildBody(actionId: actionId, decisionType: decisionType, otherValue: otherValue)

        Task {
            await sendAnswer(decisionId: decisionId, body: body)
        }
    }

    func buildBody(actionId: String, decisionType: String, otherValue: String?) -> [String: Any] {
        var body: [String: Any] = [:]

        switch decisionType {
        case "single_select", "multi_select":
            body["value"] = [actionId]
        default:
            body["value"] = actionId
        }

        if let otherValue = otherValue, !otherValue.isEmpty {
            body["other_value"] = otherValue
        }

        return body
    }

    func sendAnswer(decisionId: String, body: [String: Any]) async {
        guard let baseURL = baseURL else { return }

        do {
            let url = baseURL.appendingPathComponent("api/decisions/\(decisionId)/answer")
            var request = URLRequest(url: url)
            request.httpMethod = "POST"
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")

            let bearer = try KeychainStore.shared.readToken()
            if let bearer = bearer, !bearer.isEmpty {
                request.setValue("Bearer \(bearer)", forHTTPHeaderField: "Authorization")
            }

            request.httpBody = try JSONSerialization.data(withJSONObject: body)

            let (data, response) = try await urlSession.data(for: request)
            let httpResponse = response as? HTTPURLResponse

            switch httpResponse?.statusCode {
            case 401:
                NotificationCenter.default.post(name: .openPairingScreen, object: nil)
            case 404:
                postNotification(title: "Not found", body: "This decision could not be found.")
            case 409:
                if let data = data,
                   let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                   let detail = json["detail"] as? String,
                   detail.contains("gate") {
                    postNotification(title: "Cannot answer", body: "Gate decisions cannot be answered by device.")
                } else {
                    postAlreadyAnsweredNotification()
                }
            default: break
            }
        } catch {
        }
    }

    func sendAnswerFromInApp(decisionId: String, decisionType: String, actionId: String, otherValue: String?) async {
        let body = buildBody(actionId: actionId, decisionType: decisionType, otherValue: otherValue)
        await sendAnswer(decisionId: decisionId, body: body)
    }

    func postNotification(title: String, body: String) {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default

        let request = UNNotificationRequest(identifier: UUID().uuidString,
                                           content: content,
                                           trigger: nil)
        UNUserNotificationCenter.current().add(request) { error in
            if let error = error {
                print("Error posting notification: \(error)")
            }
        }
    }

    func postAlreadyAnsweredNotification() {
        let content = UNMutableNotificationContent()
        content.title = "Already answered"
        content.body = "This decision has already been answered."
        content.sound = .default

        let request = UNNotificationRequest(identifier: "already_answered_\(UUID().uuidString)",
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
