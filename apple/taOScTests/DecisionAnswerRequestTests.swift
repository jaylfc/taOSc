import XCTest
@testable import taOSc

final class DecisionAnswerRequestTests: XCTestCase {

    override func tearDown() {
        super.tearDown()
        try? KeychainStore.shared.deleteToken()
    }

    func testURLValidatorRejectsMissingHost() {
        let result = URLValidator.validate("http://")
        if case .failure(let error) = result {
            XCTAssertEqual(error as? URLValidator.URLValidationError, .missingHost)
        } else {
            XCTFail("Expected failure for URL with no host")
        }
    }

    func testURLValidatorAllowsHost() {
        let result = URLValidator.validate("http://evil.example.com")
        if case .success(let url) = result {
            XCTAssertEqual(url.host, "evil.example.com")
        } else {
            XCTFail("Expected success for valid URL with host")
        }
    }

    func testSendAnswerHandles401() async throws {
        let handler = DecisionNotificationHandler.shared
        handler.baseURL = URL(string: "https://test.example.com")!

        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockHTTPURLProtocol.self]
        handler.urlSession = URLSession(configuration: config)

        MockHTTPURLProtocol.mockResponses = [
            URL(string: "https://test.example.com/api/decisions/dec_123/answer")!: (401, nil)
        ]
        MockHTTPURLProtocol.capturedRequest = nil

        let body: [String: Any] = ["value": "approve"]
        await handler.sendAnswer(decisionId: "dec_123", body: body)

        XCTAssertNotNil(MockHTTPURLProtocol.capturedRequest)
    }

    func testSendAnswerFromInAppBuildsListBodyForSingleSelect() async throws {
        let handler = DecisionNotificationHandler.shared
        handler.baseURL = URL(string: "https://test.example.com")!

        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockHTTPURLProtocol.self]
        handler.urlSession = URLSession(configuration: config)

        MockHTTPURLProtocol.mockResponses = [
            URL(string: "https://test.example.com/api/decisions/dec_123/answer")!: (200, nil)
        ]
        MockHTTPURLProtocol.capturedRequest = nil

        await handler.sendAnswerFromInApp(decisionId: "dec_123", decisionType: "single_select", actionId: "opt_a", otherValue: nil)

        let request = MockHTTPURLProtocol.capturedRequest!
        let bodyData = request.httpBody!
        let json = try JSONSerialization.jsonObject(with: bodyData) as? [String: Any]
        XCTAssertEqual(json["value"] as? String, "opt_a")
        XCTAssertNil(json["other_value"])
        XCTAssertNil(json["source"])
    }

    func testSendAnswerFromInAppBuildsListBodyForMultiSelect() async throws {
        let handler = DecisionNotificationHandler.shared
        handler.baseURL = URL(string: "https://test.example.com")!

        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockHTTPURLProtocol.self]
        handler.urlSession = URLSession(configuration: config)

        MockHTTPURLProtocol.mockResponses = [
            URL(string: "https://test.example.com/api/decisions/dec_123/answer")!: (200, nil)
        ]
        MockHTTPURLProtocol.capturedRequest = nil

        await handler.sendAnswerFromInApp(decisionId: "dec_123", decisionType: "multi_select", actionId: "opt_a", otherValue: nil)

        let request = MockHTTPURLProtocol.capturedRequest!
        let bodyData = request.httpBody!
        let json = try JSONSerialization.jsonObject(with: bodyData) as? [String: Any]
        XCTAssertEqual(json["value"] as? [String], ["opt_a"])
    }

    func testSendAnswerOmitsEmptyOtherValue() async throws {
        let handler = DecisionNotificationHandler.shared
        handler.baseURL = URL(string: "https://test.example.com")!

        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockHTTPURLProtocol.self]
        handler.urlSession = URLSession(configuration: config)

        MockHTTPURLProtocol.mockResponses = [
            URL(string: "https://test.example.com/api/decisions/dec_123/answer")!: (200, nil)
        ]
        MockHTTPURLProtocol.capturedRequest = nil

        let body: [String: Any] = ["value": "approve"]
        await handler.sendAnswer(decisionId: "dec_123", body: body)

        let request = MockHTTPURLProtocol.capturedRequest!
        let bodyData = request.httpBody!
        let json = try JSONSerialization.jsonObject(with: bodyData) as? [String: Any]
        XCTAssertNil(json["other_value"])
    }

    func testSendAnswerOmitsSource() async throws {
        let handler = DecisionNotificationHandler.shared
        handler.baseURL = URL(string: "https://test.example.com")!

        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockHTTPURLProtocol.self]
        handler.urlSession = URLSession(configuration: config)

        MockHTTPURLProtocol.mockResponses = [
            URL(string: "https://test.example.com/api/decisions/dec_123/answer")!: (200, nil)
        ]
        MockHTTPURLProtocol.capturedRequest = nil

        let body: [String: Any] = ["value": "approve"]
        await handler.sendAnswer(decisionId: "dec_123", body: body)

        let request = MockHTTPURLProtocol.capturedRequest!
        let bodyData = request.httpBody!
        let json = try JSONSerialization.jsonObject(with: bodyData) as? [String: Any]
        XCTAssertNil(json["source"])
    }
}

class MockHTTPURLProtocol: URLProtocol {
    static var mockResponses: [URL: (Int, Data?)] = [:]
    static var capturedRequest: URLRequest?

    override class func canInit(with request: URLRequest) -> Bool {
        return request.url?.scheme == "http" || request.url?.scheme == "https"
    }

    override class func canonicalRequest(for request: URLRequest) -> URLRequest {
        return request
    }

    override func startLoading() {
        MockHTTPURLProtocol.capturedRequest = request
        guard let url = request.url,
              let (statusCode, body) = MockHTTPURLProtocol.mockResponses[url] else {
            let error = NSError(domain: "MockHTTPURLProtocol", code: -1, userInfo: nil)
            client?.urlProtocol(self, didFailWithError: error)
            return
        }
        let response = HTTPURLResponse(url: url, statusCode: statusCode, httpVersion: nil, headerFields: nil)!
        client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
        if let body = body {
            client?.urlProtocol(self, didLoad: body)
        }
        client?.urlProtocolDidFinishLoading(self)
    }

    override func stopLoading() {}
}
