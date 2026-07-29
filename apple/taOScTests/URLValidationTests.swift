import XCTest
@testable import taOSc

final class URLValidationTests: XCTestCase {
    func testValidHTTPSURL() {
        XCTAssertTrue(URLValidator.isValidServerURL("https://example.com"))
    }

    func testValidHTTPURL() {
        XCTAssertTrue(URLValidator.isValidServerURL("http://example.com"))
    }

    func testValidURLWithPath() {
        XCTAssertTrue(URLValidator.isValidServerURL("https://example.com/taos"))
    }

    func testValidURLWithPort() {
        XCTAssertTrue(URLValidator.isValidServerURL("https://example.com:8080"))
    }

    func testValidURLWithSubdomain() {
        XCTAssertTrue(URLValidator.isValidServerURL("https://taos.example.com"))
    }

    func testURLWithoutScheme() {
        XCTAssertTrue(URLValidator.isValidServerURL("example.com"))
    }

    func testURLWithoutSchemeWithPath() {
        XCTAssertTrue(URLValidator.isValidServerURL("example.com/taos"))
    }

    func testInvalidEmptyURL() {
        XCTAssertFalse(URLValidator.isValidServerURL(""))
    }

    func testInvalidWhitespaceOnlyURL() {
        XCTAssertFalse(URLValidator.isValidServerURL("   "))
    }

    func testInvalidSchemeFTP() {
        XCTAssertFalse(URLValidator.isValidServerURL("ftp://example.com"))
    }

    func testInvalidSchemeFile() {
        XCTAssertFalse(URLValidator.isValidServerURL("file:///path"))
    }

    func testInvalidNoHost() {
        XCTAssertFalse(URLValidator.isValidServerURL("https://"))
    }

    func testInvalidGarbage() {
        XCTAssertFalse(URLValidator.isValidServerURL("not a url at all"))
    }

    func testNormalizeAddsHTTPSPrefix() {
        XCTAssertEqual(URLValidator.normalizeURL("example.com"), "https://example.com")
    }

    func testNormalizePreservesHTTPS() {
        XCTAssertEqual(URLValidator.normalizeURL("https://example.com"), "https://example.com")
    }

    func testNormalizePreservesHTTP() {
        XCTAssertEqual(URLValidator.normalizeURL("http://example.com"), "http://example.com")
    }

    func testNormalizeTrimsWhitespace() {
        XCTAssertEqual(URLValidator.normalizeURL("  example.com  "), "https://example.com")
    }

    func testNormalizeHandlesUppercaseScheme() {
        XCTAssertEqual(URLValidator.normalizeURL("HTTPS://example.com"), "HTTPS://example.com")
    }
}
