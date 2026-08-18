import SwiftUI

struct FirstRunView: View {
    @State private var urlText: String = ""
    @State private var validationError: String?
    var onSave: (String) -> Void

    var body: some View {
        VStack(spacing: 24) {
            Text("taOS Server")
                .font(.title)
                .fontWeight(.semibold)

            Text("Enter your instance URL to connect.")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            TextField("https://taos.example.com", text: $urlText)
                .keyboardType(.URL)
                .textFieldStyle(.roundedBorder)
                .autocapitalization(.none)
                .disableAutocorrection(true)
                .padding(.horizontal)

            if let error = validationError {
                Text(error)
                    .foregroundColor(.red)
                    .font(.caption)
                    .padding(.horizontal)
            }

            Button("Connect") {
                validateAndSave()
            }
            .buttonStyle(.borderedProminent)
            .disabled(urlText.isEmpty)
        }
        .padding()
        .navigationTitle("Connect")
    }

    private func validateAndSave() {
        switch URLValidator.validate(urlText) {
        case .success:
            validationError = nil
            onSave(urlText)
        case .failure(let error):
            validationError = error.errorDescription
        }
    }
}
