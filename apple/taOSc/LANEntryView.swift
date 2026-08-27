import SwiftUI

struct LANEntryView: View {
    @State private var urlText: String = ""
    @State private var validationError: String?
    @Environment(\.dismiss) private var dismiss
    var onConnect: (URL) -> Void

    var body: some View {
        Form {
            Section(header: Text("Instance URL")) {
                TextField("https://taos.example.com", text: $urlText)
                    .keyboardType(.URL)
                    .autocapitalization(.none)
                    .disableAutocorrection(true)
            }

            if let error = validationError {
                Section {
                    Text(error)
                        .foregroundColor(.red)
                }
            }

            Button("Connect") {
                validateAndConnect()
            }
            .disabled(urlText.isEmpty)
        }
        .navigationTitle("LAN Connect")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Cancel") {
                    dismiss()
                }
            }
        }
    }

    private func validateAndConnect() {
        switch URLValidator.validate(urlText) {
        case .success(let url):
            validationError = nil
            onConnect(url)
        case .failure(let error):
            validationError = error.errorDescription
        }
    }
}
