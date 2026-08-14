import XCTest
@testable import taOSc

final class URLValidationTests: XCTestCase {
    func testValidHTTPSURL() {
        let result = URLValidator.validate("https://taos.example.com")
        switch result {
        case .success(let url):
            XCTAssertEqual(url.scheme, "https")
            XCTAssertEqual(url.host, "taos.example.com")
        case .failure:
            XCTFail("Expected success for valid HTTPS URL")
        }
    }

    func testValidHTTPURL() {
        let result = URLValidator.validate("http://192.168.1.100:8080")
        switch result {
        case .success(let url):
            XCTAssertEqual(url.scheme, "http")
            XCTAssertEqual(url.host, "192.168.1.100")
        case .failure:
            XCTFail("Expected success for valid HTTP URL")
        }
    }

    func testURLWithoutSchemeGetsHTTPS() {
        let result = URLValidator.validate("taos.example.com")
        switch result {
        case .success(let url):
            XCTAssertEqual(url.scheme, "https")
        case .failure:
            XCTFail("Expected success for URL without scheme")
        }
    }

    func testURLWithoutSchemeWithPort() {
        let result = URLValidator.validate("taos.example.com:8080")
        switch result {
        case .success(let url):
            XCTAssertEqual(url.scheme, "https")
            XCTAssertEqual(url.port, 8080)
        case .failure:
            XCTFail("Expected success for URL without scheme with port")
        }
    }

    func testEmptyURLFails() {
        let result = URLValidator.validate("")
        switch result {
        case .success:
            XCTFail("Expected failure for empty URL")
        case .failure(let error):
            XCTAssertEqual(error, .empty)
        }
    }

    func testWhitespaceOnlyURLFails() {
        let result = URLValidator.validate("   ")
        switch result {
        case .success:
            XCTFail("Expected failure for whitespace-only URL")
        case .failure(let error):
            XCTAssertEqual(error, .empty)
        }
    }

    func testURLWithOnlySchemeFails() {
        let result = URLValidator.validate("https://")
        switch result {
        case .success:
            XCTFail("Expected failure for URL with no host")
        case .failure(let error):
            XCTAssertEqual(error, .missingHost)
        }
    }

    func testFTPSchemeFails() {
        let result = URLValidator.validate("ftp://taos.example.com")
        switch result {
        case .success:
            XCTFail("Expected failure for non-http scheme")
        case .failure(let error):
            XCTAssertEqual(error, .missingScheme)
        }
    }

    func testURLWithPathIsValid() {
        let result = URLValidator.validate("https://taos.example.com/path")
        switch result {
        case .success(let url):
            XCTAssertEqual(url.path, "/path")
        case .failure:
            XCTFail("Expected success for URL with path")
        }
    }

    func testTrailingWhitespaceTrimmed() {
        let result = URLValidator.validate("  https://taos.example.com  ")
        switch result {
        case .success(let url):
            XCTAssertEqual(url.host, "taos.example.com")
        case .failure:
            XCTFail("Expected success for URL with trailing whitespace")
        }
    }
}
