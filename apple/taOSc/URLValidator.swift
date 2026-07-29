import Foundation

struct URLValidator {
    static func isValidServerURL(_ urlString: String) -> Bool {
        let trimmed = urlString.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return false }

        let normalized = normalizeURL(trimmed)
        guard let url = URL(string: normalized) else { return false }

        guard let scheme = url.scheme?.lowercased() else { return false }
        guard scheme == "http" || scheme == "https" else { return false }

        guard let host = url.host, !host.isEmpty else { return false }

        return true
    }

    static func normalizeURL(_ urlString: String) -> String {
        var trimmed = urlString.trimmingCharacters(in: .whitespacesAndNewlines)

        let lowercased = trimmed.lowercased()
        if !lowercased.hasPrefix("http://") && !lowercased.hasPrefix("https://") {
            trimmed = "https://" + trimmed
        }

        return trimmed
    }
}
