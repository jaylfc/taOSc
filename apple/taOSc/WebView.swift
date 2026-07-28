import SwiftUI
import WebKit

struct WebViewError: Identifiable, Equatable {
    let id = UUID()
    let message: String
}

struct WebView: UIViewRepresentable {
    let url: URL
    @Binding var error: WebViewError?

    class Coordinator: NSObject, WKNavigationDelegate {
        var parent: WebView
        var hasError = false

        init(_ parent: WebView) {
            self.parent = parent
        }

        func webView(
            _ webView: WKWebView,
            didFailProvisionalNavigation navigation: WKNavigation!,
            withError error: Error
        ) {
            report(error: error)
        }

        func webView(
            _ webView: WKWebView,
            didFail navigation: WKNavigation!,
            withError error: Error
        ) {
            report(error: error)
        }

        private func report(error: Error) {
            if hasError {
                return
            }
            hasError = true
            DispatchQueue.main.async {
                self.parent.error = WebViewError(
                    message: error.localizedDescription
                )
            }
        }
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    func makeUIView(context: Context) -> WKWebView {
        let config = WKWebViewConfiguration()
        config.websiteDataStore = .default()

        let webView = WKWebView(frame: .zero, configuration: config)
        webView.navigationDelegate = context.coordinator
        webView.allowsBackForwardNavigationGestures = true
        webView.scrollView.showsVerticalScrollIndicator = false

        let request = URLRequest(
            url: url,
            cachePolicy: .returnCacheDataElseLoad,
            timeoutInterval: 30
        )
        webView.load(request)
        return webView
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {}
}
