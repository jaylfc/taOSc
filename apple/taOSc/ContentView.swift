import SwiftUI

struct ContentView: View {
    @EnvironmentObject var settings: SettingsStore
    @State private var webViewError: WebViewError?
    @State private var reloadCounter = 0
    @State private var pendingDecisionOptions: [(id: String, title: String)] = []
    @State private var pendingDecisionId: String = ""
    @State private var pendingDecisionType: String = ""
    @State private var showDecisionOptions = false

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
        .sheet(isPresented: $showDecisionOptions) {
            if !pendingDecisionOptions.isEmpty {
                NavigationStack {
                    List(pendingDecisionOptions, id: \.id) { option in
                        Button(option.title) {
                            Task {
                                await DecisionNotificationHandler.shared.sendAnswerFromInApp(
                                    decisionId: pendingDecisionId,
                                    decisionType: pendingDecisionType,
                                    actionId: option.id,
                                    otherValue: nil
                                )
                            }
                            showDecisionOptions = false
                            pendingDecisionOptions = []
                        }
                    }
                    .navigationTitle("Select Option")
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) {
                            Button("Cancel") {
                                showDecisionOptions = false
                                pendingDecisionOptions = []
                            }
                        }
                    }
                }
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: .decisionTapped)) { notification in
            let options = notification.userInfo?["options"] as? [String] ?? []
            if options.isEmpty {
                return
            }
            let decisionId = notification.userInfo?["decisionId"] as? String ?? ""
            let decisionType = notification.userInfo?["decisionType"] as? String ?? ""
            pendingDecisionId = decisionId
            pendingDecisionType = decisionType
            pendingDecisionOptions = options.map { (id: $0, title: $0) }
            showDecisionOptions = true
        }
        .task {
            if settings.isPaired && settings.hasServerURL,
               let url = URL(string: settings.serverURL) {
                await PushRegistrar.shared.registerIfPaired(baseURL: url)
            }
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
