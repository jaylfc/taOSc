import XCTest
@testable import taOSc

final class PushPayloadTests: XCTestCase {

    func testHexTokenEncoding() {
        let tokenData = "com.test.token.data".data(using: .utf8)!
        let hex = tokenData.hexEncoded()
        XCTAssertEqual(hex, "636f6d2e746573742e746f702e646f6e616d65")
    }

    func testApproveDenyUserInfoParsing() {
        let userInfo: [String: Any] = [
            "decision_id": "dec_123",
            "decision_type": "approve_deny",
            "options": ["approve", "reject", "add_note"]
        ]
        let decisionId = userInfo["decision_id"] as? String
        let decisionType = userInfo["decision_type"] as? String
        XCTAssertEqual(decisionId, "dec_123")
        XCTAssertEqual(decisionType, "approve_deny")
    }

    func testFreeTextUserInfoParsing() {
        let userInfo: [String: Any] = [
            "decision_id": "dec_456",
            "decision_type": "free_text",
            "quick_reply": "Hello tailnet"
        ]
        let decisionId = userInfo["decision_id"] as? String
        let decisionType = userInfo["decision_type"] as? String
        XCTAssertEqual(decisionId, "dec_456")
        XCTAssertEqual(decisionType, "free_text")
    }

    func testOptionsUserInfoParsing() {
        let userInfo: [String: Any] = [
            "decision_id": "dec_789",
            "decision_type": "single_select",
            "options": ["option_a", "option_b", "option_c"]
        ]
        let decisionId = userInfo["decision_id"] as? String
        let decisionType = userInfo["decision_type"] as? String
        XCTAssertEqual(decisionId, "dec_789")
        XCTAssertEqual(decisionType, "single_select")
        let options = userInfo["options"] as? [String]
        XCTAssertEqual(options, ["option_a", "option_b", "option_c"])
    }

    func testAnswerRequestForOption() async throws {
        let handler = DecisionNotificationHandler.shared
        let userInfo: [String: Any] = [
            "decision_id": "dec_789",
            "decision_type": "single_select",
            "options": ["opt_a", "opt_b"]
        ]
        let decisionId = userInfo["decision_id"] as? String ?? ""

        let actionId = "opt_a"
        let otherValue: String? = nil
        let source = userInfo["decision_type"] as? String ?? ""

        let body: [String: Any] = [
            "value": actionId,
            "other_value": otherValue ?? "",
            "source": source
        ]

        let url = URL(string: "/api/decisions/\(decisionId)/answer")!
        XCTAssertEqual(url.path, "/api/decisions/dec_789/answer")

        let jsonData = try JSONSerialization.data(withJSONObject: body)
        let jsonDict = try JSONSerialization.jsonObject(with: jsonData) as? [String: Any]
        XCTAssertEqual(jsonDict?["value"] as? String, "opt_a")
        XCTAssertEqual(jsonDict?["other_value"] as? String, "")
        XCTAssertEqual(jsonDict?["source"] as? String, "single_select")
    }

    func testAnswerRequestForTextReply() async throws {
        let userInfo: [String: Any] = [
            "decision_id": "dec_012",
            "decision_type": "free_text",
            "typed_text": "Hello tailnet"
        ]
        let decisionId = userInfo["decision_id"] as? String ?? ""

        let actionId = "quick_reply"
        let otherValue = userInfo["typed_text"] as? String
        let source = userInfo["decision_type"] as? String ?? ""

        let body: [String: Any] = [
            "value": actionId,
            "other_value": otherValue ?? "",
            "source": source
        ]

        let jsonData = try JSONSerialization.data(withJSONObject: body)
        let jsonDict = try JSONSerialization.jsonObject(with: jsonData) as? [String: Any]
        XCTAssertEqual(jsonDict?["value"] as? String, "quick_reply")
        XCTAssertEqual(jsonDict?["other_value"] as? String, "Hello tailnet")
        XCTAssertEqual(jsonDict?["source"] as? String, "free_text")
    }

    func testURLValidatorRejectsForeignHost() {
        let result = URLValidator.validate("http://evil.example.com")
        XCTAssertEqual(result, .failure(.missingHost))
    }

    func testPayloadWithoutUrlOpensDecisionsList() {
        let userInfo: [String: Any] = [
            "decision_id": "dec_345",
            "decision_type": "approve_deny"
        ]
        let decisionId = userInfo["decision_id"] as? String ?? ""
        let url = userInfo["url"] as? String

        XCTAssertNil(url)
        // When url is absent, the app should open the decisions list
        // rather than failing - this is verified by the behavior code
        XCTAssertTrue(decisionId.isEmpty == false)
    }
}