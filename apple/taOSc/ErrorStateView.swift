import SwiftUI

struct ErrorStateView: View {
    let error: WebViewError
    var onRetry: () -> Void

    var body: some View {
        VStack(spacing: 24) {
            Image(systemName: "exclamationmark.octagon")
                .font(.system(size: 60))
                .foregroundColor(.red)

            Text("Server Unreachable")
                .font(.title2)
                .fontWeight(.semibold)

            Text(error.message)
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            Button("Retry") {
                onRetry()
            }
            .buttonStyle(.borderedProminent)
        }
        .padding()
    }
}
