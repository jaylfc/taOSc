import SwiftUI

struct TaOSgoEntryView: View {
    @State private var email: String = ""
    @State private var password: String = ""
    @State private var deviceName: String = ""
    @State private var viewModel: TaOSgoJoinViewModel
    @Environment(\.dismiss) private var dismiss
    var onJoinResponse: (TaOSgoJoinResponse) -> Void

    init(onJoinResponse: @escaping (TaOSgoJoinResponse) -> Void) {
        self._viewModel = State(wrappedValue: TaOSgoJoinViewModel())
        self.onJoinResponse = onJoinResponse
    }

    var body: some View {
        Group {
            switch viewModel.phase {
            case .joining:
                ProgressView("Signing in...")
            case .noInstance:
                TaOSgoNoInstanceView {
                    viewModel.phase = .idle
                }
            case .tailnetJoin(let joinKey, let loginServer, let hosts):
                ProgressView()
            case .error(let error):
                ErrorRetryView(message: error.localizedDescription) {
                    viewModel.join(
                        email: email,
                        password: password,
                        deviceName: deviceName.isEmpty ? nil : deviceName
                    )
                }
            case .idle:
                taOSgoForm
            }
        }
        .navigationTitle("taOSgo Sign In")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Cancel") {
                    dismiss()
                }
            }
        }
        .onReceive(viewModel.$phase) { newPhase in
            if case .tailnetJoin(let joinKey, let loginServer, let hosts) = newPhase {
                let response = TaOSgoJoinResponse(
                    join_key: joinKey,
                    login_server: loginServer,
                    hosts: hosts
                )
                onJoinResponse(response)
            }
        }
        .onDisappear {
            viewModel.phase = .idle
        }
    }

    private var taOSgoForm: some View {
        Form {
            Section(header: Text("Account")) {
                TextField("Email", text: $email)
                    .keyboardType(.emailAddress)
                    .autocapitalization(.none)
                    .disableAutocorrection(true)
                SecureField("Password", text: $password)
            }

            Section(header: Text("Device")) {
                TextField("Device name", text: $deviceName)
            }

            Button("Sign In") {
                viewModel.join(
                    email: email,
                    password: password,
                    deviceName: deviceName.isEmpty ? nil : deviceName
                )
            }
            .disabled(email.isEmpty || password.isEmpty)
        }
    }
}
