import Foundation

final class TaOSgoJoinViewModel: ObservableObject {
    @Published var phase: Phase

    enum Phase: Equatable {
        case idle
        case joining
        case noInstance
        case tailnetJoin(joinKey: String, loginServer: String, hosts: [TaOSgoHost])
        case error(String)
    }

    private let email: String
    private let password: String
    private let deviceName: String?

    init(email: String, password: String, deviceName: String?) {
        self.email = email
        self.password = password
        self.deviceName = deviceName
        self.phase = .joining
    }

    func join() {
        phase = .joining
        Task { @MainActor in
            do {
                let response = try await PairingService.taOSgoJoin(
                    email: email,
                    password: password,
                    deviceName: deviceName
                )
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
                phase = .error(error.localizedDescription)
            }
        }
    }
}
