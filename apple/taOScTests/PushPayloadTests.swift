import XCTest
@testable import taOSc

final class PushPayloadTests: XCTestCase {

    override func tearDown() {
        super.tearDown()
        try? KeychainStore.shared.deleteToken()
    }

    func testHexTokenEncoding() {
        let tokenData = "com.test.token.data".data(using: .utf8)!
        XCTAssertEqual(tokenData.hexEncoded, "636f6d2e746573742e746f6b656e2e64617461")
    }

    func testBuildBodyForApproveDeny() {
        let handler = DecisionNotificationHandler.shared
        let body = handler.buildBody(actionId: "approve", decisionType: "approve_deny", otherValue: nil)
        XCTAssertEqual(body["value"] as? String, "approve")
        XCTAssertNil(body["other_value"])
    }

    func testBuildBodyForSingleSelect() {
        let handler = DecisionNotificationHandler.shared
        let body = handler.buildBody(actionId: "opt_a", decisionType: "single_select", otherValue: nil)
        XCTAssertEqual(body["value"] as? String, "opt_a")
        XCTAssertNil(body["other_value"])
    }

    func testBuildBodyForMultiSelect() {
        let handler = DecisionNotificationHandler.shared
        let body = handler.buildBody(actionId: "opt_b", decisionType: "multi_select", otherValue: nil)
        XCTAssertEqual(body["value"] as? [String], ["opt_b"])
    }

    func testBuildBodyForQuickReplySendsTextAsValue() {
        let handler = DecisionNotificationHandler.shared
        let body = handler.buildBody(actionId: "quick_reply", decisionType: "free_text", otherValue: "Hello tailnet")
        XCTAssertEqual(body["value"] as? String, "Hello tailnet")
        XCTAssertNil(body["other_value"])
    }

    func testBuildBodyForAddNoteReturnsEmptyBody() {
        let handler = DecisionNotificationHandler.shared
        let body = handler.buildBody(actionId: "add_note", decisionType: "free_text", otherValue: "A note")
        XCTAssertNil(body["value"])
        XCTAssertNil(body["other_value"])
    }

    func testBuildBodyOmitsEmptyOtherValue() {
        let handler = DecisionNotificationHandler.shared
        let body = handler.buildBody(actionId: "approve", decisionType: "approve_deny", otherValue: "")
        XCTAssertNil(body["other_value"])
    }

    func testBuildBodyOmitsSource() {
        let handler = DecisionNotificationHandler.shared
        let body = handler.buildBody(actionId: "approve", decisionType: "approve_deny", otherValue: nil)
        XCTAssertNil(body["source"])
    }

    func testSendAnswerUsesBaseURL() async throws {
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
        XCTAssertEqual(request.httpMethod, "POST")
        XCTAssertEqual(request.url?.path, "/api/decisions/dec_123/answer")
        XCTAssertEqual(request.value(forHTTPHeaderField: "Content-Type"), "application/json")
    }

    func testSendAnswerIncludesAuthorizationHeader() async throws {
        let handler = DecisionNotificationHandler.shared
        handler.baseURL = URL(string: "https://test.example.com")!

        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockHTTPURLProtocol.self]
        handler.urlSession = URLSession(configuration: config)

        try KeychainStore.shared.saveToken("test_scoped_token")

        MockHTTPURLProtocol.mockResponses = [
            URL(string: "https://test.example.com/api/decisions/dec_123/answer")!: (200, nil)
        ]
        MockHTTPURLProtocol.capturedRequest = nil

        let body: [String: Any] = ["value": "approve"]
        await handler.sendAnswer(decisionId: "dec_123", body: body)

        let request = MockHTTPURLProtocol.capturedRequest!
        XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer test_scoped_token")
    }

    func testSendAnswerOmitsOtherValueWhenEmpty() async throws {
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

    func testSendAnswerHandles404() async throws {
        let handler = DecisionNotificationHandler.shared
        handler.baseURL = URL(string: "https://test.example.com")!

        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockHTTPURLProtocol.self]
        handler.urlSession = URLSession(configuration: config)

        MockHTTPURLProtocol.mockResponses = [
            URL(string: "https://test.example.com/api/decisions/dec_123/answer")!: (404, nil)
        ]
        MockHTTPURLProtocol.capturedRequest = nil

        let body: [String: Any] = ["value": "approve"]
        await handler.sendAnswer(decisionId: "dec_123", body: body)

        XCTAssertNotNil(MockHTTPURLProtocol.capturedRequest)
        XCTAssertEqual(MockHTTPURLProtocol.capturedRequest?.httpMethod, "POST")
    }

    func testSendAnswerHandles409Gate() async throws {
        let handler = DecisionNotificationHandler.shared
        handler.baseURL = URL(string: "https://test.example.com")!

        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockHTTPURLProtocol.self]
        handler.urlSession = URLSession(configuration: config)

        let gateBody = try JSONSerialization.data(withJSONObject: ["detail": "gate decisions cannot be answered by a device bearer"])
        MockHTTPURLProtocol.mockResponses = [
            URL(string: "https://test.example.com/api/decisions/dec_123/answer")!: (409, gateBody)
        ]
        MockHTTPURLProtocol.capturedRequest = nil

        let body: [String: Any] = ["value": "approve"]
        await handler.sendAnswer(decisionId: "dec_123", body: body)

        XCTAssertNotNil(MockHTTPURLProtocol.capturedRequest)
    }

    func testSendAnswerHandles409AlreadyAnswered() async throws {
        let handler = DecisionNotificationHandler.shared
        handler.baseURL = URL(string: "https://test.example.com")!

        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockHTTPURLProtocol.self]
        handler.urlSession = URLSession(configuration: config)

        MockHTTPURLProtocol.mockResponses = [
            URL(string: "https://test.example.com/api/decisions/dec_123/answer")!: (409, nil)
        ]
        MockHTTPURLProtocol.capturedRequest = nil

        let body: [String: Any] = ["value": "approve"]
        await handler.sendAnswer(decisionId: "dec_123", body: body)

        XCTAssertNotNil(MockHTTPURLProtocol.capturedRequest)
    }

    func testRegisterCategoriesFromOptionsPayload() async throws {
        let handler = DecisionNotificationHandler.shared
        let userInfo: [String: Any] = [
            "decision_type": "single_select",
            "options": ["opt_a", "opt_b"]
        ]

        handler.registerCategories(from: userInfo)

        let categories = try await getCategories()
        let category = categories.first(where: { $0.identifier == DecisionCategory.options.rawValue })
        XCTAssertEqual(category?.actions.count, 2)
        XCTAssertEqual(category?.actions.first?.identifier, "opt_a")
        XCTAssertEqual(category?.actions.last?.identifier, "opt_b")
    }

    func testPushRegistrarSendsTokenWithAuthorization() async throws {
        let handler = PushRegistrar.shared
        handler.baseURL = URL(string: "https://test.example.com")!
        handler.deviceId = "test-device-id"

        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockHTTPURLProtocol.self]
        handler.urlSession = URLSession(configuration: config)

        try KeychainStore.shared.saveToken("test_scoped_token")

        MockHTTPURLProtocol.mockResponses = [
            URL(string: "https://test.example.com/api/devices/test-device-id/push-token")!: (200, nil)
        ]
        MockHTTPURLProtocol.capturedRequest = nil

        let token = Data("test".utf8)
        handler.setDeviceToken(token)

        let start = Date()
        while MockHTTPURLProtocol.capturedRequest == nil && Date().timeIntervalSince(start) < 1.0 {
            try await Task.sleep(nanoseconds: 10_000_000)
        }

        let request = MockHTTPURLProtocol.capturedRequest!
        XCTAssertEqual(request.httpMethod, "PATCH")
        XCTAssertEqual(request.url?.path, "/api/devices/test-device-id/push-token")
        XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer test_scoped_token")
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

private func getCategories() async throws -> Set<UNNotificationCategory> {
    try await withCheckedThrowingContinuation { continuation in
        UNUserNotificationCenter.current().getNotificationCategories { categories in
            continuation.resume(returning: categories)
        }
    }
}
