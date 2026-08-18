import Foundation

enum URLValidationError: Error, LocalizedError, Equatable {
    case empty
    case missingScheme
    case missingHost
    case invalidFormat

    var errorDescription: String? {
        switch self {
        case .empty:
            return "Server URL is required."
        case .missingScheme:
            return "URL must use http or https."
        case .missingHost:
            return "URL must include a host."
        case .invalidFormat:
            return "URL is not valid."
        }
    }
}

struct URLValidator {
    static func validate(_ urlString: String) -> Result<URL, URLValidationError> {
        let trimmed = urlString.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty {
            return .failure(.empty)
        }

        let normalized: String
        if trimmed.contains("://") {
            normalized = trimmed
        } else {
            normalized = "https://" + trimmed
        }

        guard let url = URL(string: normalized) else {
            return .failure(.invalidFormat)
        }

        guard let scheme = url.scheme?.lowercased(),
              scheme == "http" || scheme == "https" else {
            return .failure(.missingScheme)
        }

        guard url.host != nil else {
            return .failure(.missingHost)
        }

        return .success(url)
    }
}
