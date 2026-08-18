import SwiftUI

struct SettingsView: View {
    @EnvironmentObject var settings: SettingsStore
    @Environment(\.dismiss) var dismiss
    @State private var urlText: String = ""
    @State private var validationError: String?

    var body: some View {
        NavigationStack {
            Form {
                Section(header: Text("Server")) {
                    TextField("Server URL", text: $urlText)
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
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        save()
                    }
                }
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
            }
            .onAppear {
                urlText = settings.serverURL
            }
        }
    }

    private func save() {
        switch URLValidator.validate(urlText) {
        case .success:
            validationError = nil
            settings.saveServerURL(urlText)
            dismiss()
        case .failure(let error):
            validationError = error.errorDescription
        }
    }
}
