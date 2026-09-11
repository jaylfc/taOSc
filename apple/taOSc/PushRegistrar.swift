import UIKit
import Foundation
import UserNotifications

final class PushRegistrar {
    static let shared = PushRegistrar()
    private init() {}

    var deviceToken: Data?
    var baseURL: URL?
    private var deviceId: String?
    var urlSession: URLSession = .shared

    func registerIfPaired(baseURL: URL) async {
        do {
            let deviceId = try KeychainStore.shared.readDeviceId()
            guard let deviceId = deviceId, !deviceId.isEmpty else { return }

            self.deviceId = deviceId
            self.baseURL = baseURL
            DecisionNotificationHandler.baseURL = baseURL

            let authorized = try await UNUserNotificationCenter.current()
                .requestAuthorization(options: [.alert, .sound, .badge])
            guard authorized else { return }

            await MainActor.run {
                UIApplication.shared.registerForRemoteNotifications()
            }
        } catch {
            return
        }
    }

    func setDeviceToken(_ token: Data) {
        deviceToken = token
        guard let baseURL = baseURL, let deviceId = deviceId else { return }
        Task {
            await sendTokenIfAvailable(to: baseURL, deviceId: deviceId)
        }
    }

    private func sendTokenIfAvailable(to baseURL: URL, deviceId: String) async {
        guard let token = deviceToken else { return }
        let hex = token.hexEncoded
        do {
            try await sendToken(to: baseURL, deviceId: deviceId, token: hex)
        } catch {
        }
    }

    private func sendToken(to baseURL: URL, deviceId: String, token: String) async throws {
        let url = baseURL.appendingPathComponent("api/devices/\(deviceId)/push-token")
        var request = URLRequest(url: url)
        request.httpMethod = "PATCH"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        let scopedToken = try KeychainStore.shared.readToken()
        if let scopedToken = scopedToken, !scopedToken.isEmpty {
            request.setValue("Bearer \(scopedToken)", forHTTPHeaderField: "Authorization")
        }

        let body: [String: Any] = ["push_token": token]
        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        let (_, response) = try await urlSession.data(for: request)
        let httpResponse = response as? HTTPURLResponse

        if httpResponse?.statusCode == 401 {
            NotificationCenter.default.post(name: .openPairingScreen, object: nil)
        }

        try KeychainStore.shared.savePushTokenHex(token)
    }
}
