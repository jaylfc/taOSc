import SwiftUI

struct TaOSgoEntryView: View {
    @State private var email: String = ""
    @State private var password: String = ""
    @State private var deviceName: String = ""
    @State private var viewModel: TaOSgoJoinViewModel?
    @Environment(\.dismiss) private var dismiss
    var onJoinResponse: (TaOSgoJoinResponse) -> Void

    var body: some View {
        Group {
            if let viewModel = viewModel {
                switch viewModel.phase {
                case .joining:
                    ProgressView("Signing in...")
                case .noInstance:
                    TaOSgoNoInstanceView {
                        viewModel.phase = .idle
                    }
                case .tailnetJoin(let joinKey, let loginServer, let hosts):
                    EmptyView()
                case .error(let message):
                    ErrorRetryView(message: message) {
                        viewModel.join()
                    }
                case .idle:
                    taOSgoForm
                        .environment(\.dismiss, dismiss)
                }
            } else {
                taOSgoForm
                    .environment(\.dismiss, dismiss)
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
        .onChange(of: viewModel?.phase) { _, newPhase in
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
            viewModel?.phase = .idle
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
                let vm = TaOSgoJoinViewModel(
                    email: email,
                    password: password,
                    deviceName: deviceName.isEmpty ? nil : deviceName
                )
                self.viewModel = vm
                vm.join()
            }
            .disabled(email.isEmpty || password.isEmpty)
        }
    }
}
