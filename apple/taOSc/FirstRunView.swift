import SwiftUI

enum LoginMethod: Hashable {
    case lan
    case taOSgo
}

struct FirstRunView: View {
    @State private var path = NavigationPath()
    @Binding var serverURL: String
    @EnvironmentObject var errorState: ErrorStateStore

    var body: some View {
        NavigationStack(path: $path) {
            LoginMethodChooserView { method in
                path.append(method)
            }
            .navigationDestination(for: LoginMethod.self) { method in
                switch method {
                case .lan:
                    LANEntryView { url in
                        path.append(PairingGrantDestination(url: url, displayName: "iOS Device"))
                    }
                case .taOSgo:
                    TaOSgoEntryView { joinResponse in
                        path.append(TailnetJoinDestination(response: joinResponse))
                    }
                }
            }
            .navigationDestination(for: PairingGrantDestination.self) { destination in
                PairingGrantView(
                    baseURL: destination.url,
                    displayName: destination.displayName
                ) {
                    serverURL = destination.url.absoluteString
                } onDismiss: {
                    path.removeLast()
                }
            }
            .navigationDestination(for: TailnetJoinDestination.self) { destination in
                if let host = destination.response.hosts.first,
                   let instanceURL = URL(string: host.addr) {
                    TailnetJoinView(
                        joinKey: destination.response.join_key,
                        loginServer: destination.response.login_server,
                        instanceURL: instanceURL
                    ) { instanceURL in
                        path.append(PairingGrantDestination(url: instanceURL, displayName: "iOS Device"))
                    }
                } else {
                    ErrorDestinationView(
                        message: "Could not reach the instance: the join response contained no usable host address.",
                        onBack: { path.removeLast() }
                    )
                }
            }
            .navigationDestination(for: ErrorDestination.self) { destination in
                ErrorDestinationView(message: destination.message) {
                    path.removeLast()
                }
            }
        }
    }
}

struct PairingGrantDestination: Hashable {
    let url: URL
    let displayName: String
}

struct ErrorDestination: Hashable {
    let message: String
}

struct ErrorDestinationView: View {
    let message: String
    @EnvironmentObject var errorState: ErrorStateStore
    let onBack: () -> Void
    
    var body: some View {
        TerminalStateView(
            icon: "xmark.circle.fill",
            iconColor: .red,
            title: "Connection Error",
            message: message,
            buttonTitle: "Back",
            action: {
                errorState.clearError()
                onBack()
            }
        )
    }
}

final class ErrorStateStore: ObservableObject {
    @Published var errorMessage: String? = nil
    
    func showError(_ message: String) {
        errorMessage = message
    }
    
    func clearError() {
        errorMessage = nil
    }
}

struct TailnetJoinDestination: Hashable {
    let response: TaOSgoJoinResponse
}

struct LoginMethodChooserView: View {
    var onSelect: (LoginMethod) -> Void

    var body: some View {
        VStack(spacing: 24) {
            Text("Sign In")
                .font(.title)
                .fontWeight(.semibold)

            Text("Choose how to connect to your taOS instance.")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            VStack(spacing: 16) {
                Button {
                    onSelect(.lan)
                } label: {
                    VStack(spacing: 8) {
                        Image(systemName: "network")
                            .font(.system(size: 40))
                        Text("Connect via LAN")
                            .font(.headline)
                        Text("Enter your instance URL directly")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(.ultraThinMaterial)
                    .cornerRadius(12)
                }
                .buttonStyle(.plain)

                Button {
                    onSelect(.taOSgo)
                } label: {
                    VStack(spacing: 8) {
                        Image(systemName: "cloud.fill")
                            .font(.system(size: 40))
                        Text("Sign in with taOSgo")
                            .font(.headline)
                        Text("Use your taOSgo account")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(.ultraThinMaterial)
                    .cornerRadius(12)
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal)
        }
        .padding()
        .navigationTitle("Connect")
        .navigationBarTitleDisplayMode(.inline)
    }
}
