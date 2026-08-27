import SwiftUI

struct TailnetJoinView: View {
    let joinKey: String
    let loginServer: String
    let instanceURL: URL
    let onJoined: (URL) -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        Form {
            Section(header: Text("Tailnet Join Key")) {
                Text(joinKey)
                    .font(.system(.body, design: .monospaced))
            }

            Section(header: Text("Login Server")) {
                Text(loginServer)
                    .font(.system(.body, design: .monospaced))
                    .foregroundColor(.secondary)
            }

            Section(header: Text("Instance")) {
                Text(instanceURL.absoluteString)
                    .font(.system(.body, design: .monospaced))
                    .foregroundColor(.secondary)
            }

            Section(header: Text("Instructions")) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("1. Open the Tailscale app on this device.")
                    Text("2. Use the login key above to authenticate against the login server.")
                    Text("3. Wait until the tailnet connection is active.")
                    Text("4. Tap Continue to run the grant flow against the instance.")
                }
                .font(.subheadline)
                .foregroundColor(.secondary)
            }

            Button("Continue") {
                onJoined(instanceURL)
            }
        }
        .navigationTitle("Join Tailnet")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Cancel") {
                    dismiss()
                }
            }
        }
    }
}
