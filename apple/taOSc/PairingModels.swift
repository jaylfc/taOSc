import Foundation

struct PairRequestResponse: Codable, Equatable {
    let pair_request_id: String
    let verify_code: String
}

struct PairRequestPollResponse: Codable, Equatable {
    let id: String
    let status: String
    let scoped_token: String?

    var requestStatus: PairRequestStatus? {
        switch status.lowercased() {
        case "pending":
            return .pending
        case "approved":
            if let token = scoped_token {
                return .approved(token)
            }
            return nil
        case "denied":
            return .denied
        case "expired":
            return .expired
        default:
            return nil
        }
    }
}

enum PairRequestStatus: Equatable {
    case pending
    case approved(String)
    case denied
    case expired
}

struct TaOSgoJoinResponse: Codable, Equatable, Hashable {
    let join_key: String
    let login_server: String
    let hosts: [TaOSgoHost]
}

struct TaOSgoHost: Codable, Equatable, Hashable {
    let handle: String
    let addr: String
}

enum PairingError: LocalizedError, Equatable {
    case unreachable
    case invalidResponse
    case unknown

    var errorDescription: String? {
        switch self {
        case .unreachable:
            return "Cannot reach the server."
        case .invalidResponse:
            return "Invalid response from server."
        case .unknown:
            return "An unknown error occurred."
        }
    }
}
