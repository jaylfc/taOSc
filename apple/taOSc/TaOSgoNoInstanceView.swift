import SwiftUI

struct TaOSgoNoInstanceView: View {
    let onContinue: () -> Void

    var body: some View {
        VStack(spacing: 24) {
            Image(systemName: "person.badge.plus")
                .font(.system(size: 60))
                .foregroundColor(.blue)

            Text("No taOS Instance Found")
                .font(.title2)
                .fontWeight(.semibold)

            Text("Your taOSgo account exists but has no associated instance. To connect, please create a taOS instance from your taOSgo dashboard.")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            VStack(spacing: 12) {
                Text("1. Log in to your taOSgo account at app.taOSgo.com")
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)

                Text("2. Navigate to 'My Instances'")
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)

                Text("3. Click 'Create Instance' and follow the setup instructions")
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }

            Button("Return to Sign In") {
                onContinue()
            }
            .buttonStyle(.borderedProminent)
            .padding(.top)
        }
        .padding()
    }
}