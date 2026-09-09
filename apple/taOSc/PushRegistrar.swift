import Foundation
import UserNotifications

final class PushRegistrar {
    static let shared = PushRegistrar()
    private init() {}

    var deviceToken: Data?

    func registerIfPaired(baseURL: URL) async {
        do {
            let deviceId = try KeychainStore.shared.readDeviceId()
            if deviceId.isEmpty {
                return
            }

            let authorized = try await UNUserNotificationCenter.current()
                .requestAuthorization(options: [.alert, .sound, .badge])
            guard authorized else { return }

            UIApplication.shared.registerForRemoteNotifications()

            await MainActor.run {
                self.sendTokenIfAvailable(to: baseURL, deviceId: deviceId)
            }
        } catch {
            return
        }
    }

    func setDeviceToken(_ token: Data) {
        deviceToken = token
    }

    private func sendTokenIfAvailable(to baseURL: URL, deviceId: String) {
        guard let token = deviceToken else { return }

        let hex = token.hexEncoded()
        sendToken(to: baseURL, deviceId: deviceId, token: hex)
    }

    private func sendToken(to baseURL: URL, deviceId: String, token: String) {
        let url = baseURL.appendingPathComponent("api/devices/\(deviceId)/push-token")
        var request = URLRequest(url: url)
        request.httpMethod = "PATCH"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        let body: [String: Any] = ["push_token": token]
        request.httpBody = try? JSONSerialization.data(withJSONObject: body)

        _ = try? await URLSession.shared.data(for: request)

        KeychainStore.shared.savePushTokenHex(token)
    }
}