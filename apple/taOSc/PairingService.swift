import Foundation

enum PairingService {
    static func createPairRequest(
        baseURL: URL,
        platform: String,
        displayName: String
    ) async throws -> PairRequestResponse {
        let url = baseURL.appendingPathComponent("api/devices/pair-requests")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        let body: [String: Any] = [
            "platform": platform,
            "display_name": displayName
        ]
        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        let (data, response) = try await URLSession.shared.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            throw PairingError.unreachable
        }
        return try JSONDecoder().decode(PairRequestResponse.self, from: data)
    }

    static func pollPairRequest(
        baseURL: URL,
        requestId: String
    ) async throws -> PairRequestStatus {
        let url = baseURL.appendingPathComponent("api/devices/pair-requests/\(requestId)")
        var request = URLRequest(url: url)
        request.httpMethod = "GET"

        let (data, response) = try await URLSession.shared.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse else {
            throw PairingError.unreachable
        }

        switch httpResponse.statusCode {
        case 200:
            let pollResponse = try JSONDecoder().decode(PairRequestPollResponse.self, from: data)
            guard let status = pollResponse.requestStatus else {
                throw PairingError.invalidResponse
            }
            return status
        case 404:
            throw PairingError.invalidResponse
        default:
            throw PairingError.unreachable
        }
    }

    static func taOSgoJoin(
        email: String,
        password: String,
        deviceName: String?
    ) async throws -> TaOSgoJoinResponse {
        // tsk-5rukhy: website endpoint not yet shipped; build against documented shape.
        guard let url = URL(string: "https://taos.my/api/taosgo/app-join") else {
            throw PairingError.unreachable
        }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        var body: [String: Any] = [
            "email": email,
            "password": password
        ]
        if let deviceName = deviceName, !deviceName.isEmpty {
            body["device_name"] = deviceName
        }
        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        let (data, response) = try await URLSession.shared.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            throw PairingError.unreachable
        }
        return try JSONDecoder().decode(TaOSgoJoinResponse.self, from: data)
    }
}
