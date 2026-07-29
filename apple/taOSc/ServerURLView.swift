import SwiftUI

struct ServerURLView: View {
    @State private var urlText: String = ""
    @State private var showError: Bool = false
    @FocusState private var isFocused: Bool

    let onSave: (String) -> Void

    var body: some View {
        VStack(spacing: 24) {
            VStack(spacing: 8) {
                Text("taOS Server URL")
                    .font(.title2)
                    .fontWeight(.semibold)

                Text("Enter your taOS instance URL to get started.")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }

            TextField("https://example.com", text: $urlText, onCommit: save)
                .textFieldStyle(.roundedBorder)
                .keyboardType(.URL)
                .autocapitalization(.none)
                .disableAutocorrection(true)
                .focused($isFocused)
                .padding(.horizontal)

            if showError {
                Text("Please enter a valid URL (e.g. https://example.com)")
                    .font(.caption)
                    .foregroundColor(.red)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)
            }

            Button("Connect") {
                save()
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
            .disabled(urlText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)

            Spacer()
        }
        .padding()
        .onAppear {
            isFocused = true
        }
    }

    private func save() {
        if URLValidator.isValidServerURL(urlText) {
            showError = false
            onSave(urlText)
        } else {
            showError = true
        }
    }
}
