import SwiftUI

struct ContentView: View {
    @EnvironmentObject var settings: SettingsStore
    @State private var webViewError: WebViewError?
    @State private var reloadCounter = 0

    var body: some View {
        NavigationStack {
            Group {
                if settings.hasServerURL, settings.isPaired, let url = validatedURL {
                    WebView(url: url, error: $webViewError)
                        .ignoresSafeArea()
                        .id("\(url.absoluteString)-\(reloadCounter)")
                        .navigationBarTitleDisplayMode(.inline)
                        .toolbar {
                            ToolbarItem(placement: .navigationBarTrailing) {
                                Button {
                                    settings.showSettings = true
                                } label: {
                                    Image(systemName: "gear")
                                }
                                .accessibilityLabel("Settings")
                            }
                        }
                } else {
                    FirstRunView(serverURL: $settings.serverURL)
                }
            }
        }
        .fullScreenCover(item: $webViewError) { error in
            ErrorStateView(error: error) {
                webViewError = nil
                reloadCounter += 1
            }
        }
        .sheet(isPresented: $settings.showSettings) {
            SettingsView()
                .environmentObject(settings)
        }
    }

    private var validatedURL: URL? {
        switch URLValidator.validate(settings.serverURL) {
        case .success(let url):
            return url
        case .failure:
            return nil
        }
    }
}
