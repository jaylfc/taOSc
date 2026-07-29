import SwiftUI

struct ContentView: View {
    @EnvironmentObject var settings: SettingsManager
    @State private var showError: Bool = false
    @State private var errorMessage: String = ""
    @State private var reloadTrigger: Int = 0
    @State private var showSettings: Bool = false

    private var serverURL: URL? {
        URL(string: settings.serverURL)
    }

    var body: some View {
        Group {
            if !settings.hasServerURL {
                ServerURLView { url in
                    settings.save(url: url)
                }
            } else if let url = serverURL {
                ZStack {
                    WebView(
                        url: url,
                        showError: $showError,
                        errorMessage: $errorMessage,
                        reloadTrigger: $reloadTrigger
                    )
                    .ignoresSafeArea()

                    if showError {
                        ErrorView(message: errorMessage) {
                            reloadTrigger += 1
                            showError = false
                            errorMessage = ""
                        }
                    }
                }
                .overlay(
                    Button(action: { showSettings = true }) {
                        Image(systemName: "gearshape")
                            .font(.title2)
                            .padding(8)
                            .background(.ultraThinMaterial, in: Circle())
                    }
                    .padding(),
                    alignment: .topTrailing
                )
            } else {
                ServerURLView { url in
                    settings.save(url: url)
                }
            }
        }
        .sheet(isPresented: $showSettings) {
            SettingsView(settings: settings)
        }
        .onChange(of: settings.serverURL) { _ in
            showError = false
            errorMessage = ""
        }
    }
}
