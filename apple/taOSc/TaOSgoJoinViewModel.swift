import Foundation

@MainActor
final class TaOSgoJoinViewModel: ObservableObject {
    @Published var phase: Phase

    enum Phase {
        case idle
        case joining
        case noInstance
        case tailnetJoin(joinKey: String, loginServer: String, hosts: [TaOSgoHost])
        case error(Error)
    }

    private var email: String
    private var password: String
    private var deviceName: String?
    private var joinTask: Task<Void, Never>?

    init(email: String = "", password: String = "", deviceName: String? = nil) {
        self.email = email
        self.password = password
        self.deviceName = deviceName
        self.phase = .idle
    }

    func join(email: String, password: String, deviceName: String?) {
        self.email = email
        self.password = password
        self.deviceName = deviceName
        joinTask?.cancel()
        phase = .joining
        joinTask = Task { @MainActor in
            do {
                let response = try await PairingService.taOSgoJoin(
                    email: email,
                    password: password,
                    deviceName: deviceName
                )
                guard !response.join_key.isEmpty, !response.login_server.isEmpty else {
                    throw PairingError.invalidResponse
                }
                if response.hosts.isEmpty {
                    phase = .noInstance
                } else {
                    phase = .tailnetJoin(
                        joinKey: response.join_key,
                        loginServer: response.login_server,
                        hosts: response.hosts
                    )
                }
            } catch {
                phase = .error(error)
            }
        }
    }
}
