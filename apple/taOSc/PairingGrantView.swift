import SwiftUI

final class PairingGrantViewModel: ObservableObject {
    @Published var phase: Phase
    private let baseURL: URL
    private let displayName: String
    private var pollTask: Task<Void, Never>?

    enum Phase: Equatable {
        case creating
        case pending(verifyCode: String)
        case approved
        case denied
        case expired
        case unreachable(String)
    }

    init(baseURL: URL, displayName: String) {
        self.baseURL = baseURL
        self.displayName = displayName
        self.phase = .creating
    }

    func start() {
        guard phase == .creating else { return }
        pollTask?.cancel()
        pollTask = Task { @MainActor in
            do {
                let response = try await PairingService.createPairRequest(
                    baseURL: baseURL,
                    platform: "ios",
                    displayName: displayName
                )
                self.phase = .pending(verifyCode: response.verify_code)
                await poll(requestId: response.pair_request_id)
            } catch {
                self.phase = .unreachable(error.localizedDescription)
            }
        }
    }

    func cancel() {
        pollTask?.cancel()
    }

    deinit {
        pollTask?.cancel()
    }

    private func poll(requestId: String) async {
        while !Task.isCancelled {
            do {
                let status = try await PairingService.pollPairRequest(
                    baseURL: baseURL,
                    requestId: requestId
                )
                switch status {
                case .pending:
                    try? await Task.sleep(nanoseconds: 3_000_000_000)
                case .approved(let token):
                    try? KeychainStore.shared.saveToken(token)
                    self.phase = .approved
                    return
                case .denied:
                    self.phase = .denied
                    return
                case .expired:
                    self.phase = .expired
                    return
                }
            } catch {
                self.phase = .unreachable(error.localizedDescription)
                return
            }
        }
    }
}

struct PairingGrantView: View {
    @StateObject private var viewModel: PairingGrantViewModel
    let onComplete: () -> Void
    let onDismiss: () -> Void

    init(baseURL: URL, displayName: String, onComplete: @escaping () -> Void, onDismiss: @escaping () -> Void) {
        _viewModel = StateObject(wrappedValue: PairingGrantViewModel(
            baseURL: baseURL,
            displayName: displayName
        ))
        self.onComplete = onComplete
        self.onDismiss = onDismiss
    }

    var body: some View {
        Group {
            switch viewModel.phase {
            case .creating:
                ProgressView("Creating pairing request...")
                    .onAppear {
                        viewModel.start()
                    }

            case .pending(let verifyCode):
                PendingGrantView(verifyCode: verifyCode)

            case .approved:
                ProgressView("Pairing approved. Opening...")
                    .onAppear {
                        onComplete()
                    }

            case .denied:
                TerminalStateView(
                    icon: "xmark.circle.fill",
                    iconColor: .red,
                    title: "Pairing Denied",
                    message: "The request was denied. Please try again.",
                    buttonTitle: "Back",
                    action: { onDismiss() }
                )

            case .expired:
                TerminalStateView(
                    icon: "clock.badge.exclamationmark.fill",
                    iconColor: .orange,
                    title: "Pairing Expired",
                    message: "The verification code has expired. Please try again.",
                    buttonTitle: "Back",
                    action: { onDismiss() }
                )

            case .unreachable(let message):
                ErrorRetryView(message: message) {
                    viewModel.phase = .creating
                    viewModel.start()
                }
            }
        }
        .navigationTitle("Pairing")
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct PendingGrantView: View {
    let verifyCode: String

    var body: some View {
        VStack(spacing: 24) {
            Text("Compare this code with the one shown on your taOS instance.")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            Text(verifyCode)
                .font(.system(.title, design: .monospaced))
                .fontWeight(.bold)
                .tracking(8)
                .padding()
                .frame(maxWidth: .infinity)
                .background(.ultraThinMaterial)
                .cornerRadius(12)
                .padding(.horizontal)

            ProgressView("Waiting for approval...")
                .padding()
        }
        .padding()
    }
}

struct TerminalStateView: View {
    let icon: String
    let iconColor: Color
    let title: String
    let message: String
    let buttonTitle: String
    let action: () -> Void

    var body: some View {
        VStack(spacing: 24) {
            Image(systemName: icon)
                .font(.system(size: 60))
                .foregroundColor(iconColor)

            Text(title)
                .font(.title2)
                .fontWeight(.semibold)

            Text(message)
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            Button(buttonTitle) {
                action()
            }
            .buttonStyle(.borderedProminent)
        }
        .padding()
    }
}

struct ErrorRetryView: View {
    let message: String
    let retry: () -> Void

    var body: some View {
        VStack(spacing: 24) {
            Image(systemName: "exclamationmark.octagon")
                .font(.system(size: 60))
                .foregroundColor(.red)

            Text("Server Unreachable")
                .font(.title2)
                .fontWeight(.semibold)

            Text(message)
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            Button("Retry") {
                retry()
            }
            .buttonStyle(.borderedProminent)
        }
        .padding()
    }
}
