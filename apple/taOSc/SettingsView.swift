import SwiftUI

struct SettingsView: View {
    @Environment(\.dismiss) var dismiss
    @ObservedObject var settings: SettingsManager
    @State private var urlText: String = ""
    @State private var showError: Bool = false

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Server")) {
                    TextField("https://example.com", text: $urlText, onCommit: save)
                        .textFieldStyle(.roundedBorder)
                        .keyboardType(.URL)
                        .autocapitalization(.none)
                        .disableAutocorrection(true)
                }

                if showError {
                    Section {
                        Text("Please enter a valid URL (e.g. https://example.com)")
                            .font(.caption)
                            .foregroundColor(.red)
                    }
                }

                Section {
                    Button("Save") {
                        save()
                    }
                    .disabled(urlText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
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
        if URLValidator.isValidServerURL(urlText) {
            settings.save(url: urlText)
            dismiss()
        } else {
            showError = true
        }
    }
}
