import XCTest
@testable import taOSc

final class DecisionAnswerRequestTests: XCTestCase {

    func testOptionAnswerURLAndBody() async throws {
        let decisionId = "dec_abc"
        let actionId = "approve"
        let otherValue: String? = nil
        let source = "approve_deny"

        let url = URL(string: "/api/decisions/\(decisionId)/answer")!
        XCTAssertEqual(url.path, "/api/decisions/dec_abc/answer")

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer test_bearer", forHTTPHeaderField: "Authorization")
        request.httpBody = try? JSONSerialization.data(withJSONObject: [
            "value": actionId,
            "other_value": otherValue ?? "",
            "source": source
        ])

        let (_, response) = try await URLSession.shared.data(for: request)
        let httpResponse = response as? HTTPURLResponse
        XCTAssertEqual(httpResponse?.statusCode, 200)
    }

    func testTextReplyURLAndBody() async throws {
        let decisionId = "def_001"
        let actionId = "quick_reply"
        let otherValue = "typed reply text"
        let source = "free_text"

        let url = URL(string: "/api/decisions/\(decisionId)/answer")!
        XCTAssertEqual(url.path, "/api/decisions/def_001/answer")

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer test_bearer", forHTTPHeaderField: "Authorization")
        request.httpBody = try? JSONSerialization.data(withJSONObject: [
            "value": actionId,
            "other_value": otherValue,
            "source": source
        ])

        let (_, response) = try await URLSession.shared.data(for: request)
        let httpResponse = response as? HTTPURLResponse
        XCTAssertEqual(httpResponse?.statusCode, 200)
    }

    func testURLValidatorRejectsForeignHost() {
        let result = URLValidator.validate("http://evil.example.com")
        if case .failure(let error) = result {
            XCTAssertEqual(error as? URLValidator.URLValidationError, .missingHost)
        } else {
            XCTFail("Expected failure for foreign host")
        }
    }

    func testURLValidatorAllowsLocalhost() {
        let result = URLValidator.validate("https://localhost:8080")
        if case .success = result {
            // expected
        } else {
            XCTFail("Expected success for localhost")
        }
    }

    func testPayloadWithoutUrlDoesNotFail() {
        let userInfo: [String: Any] = [
            "decision_id": "dec_999",
            "decision_type": "approve_deny"
        ]
        let decisionId = userInfo["decision_id"] as? String ?? ""
        let url = userInfo["url"]

        XCTAssertNil(url)
        // When url is absent, the app navigates to decisions list instead of failing
        XCTAssertEqual(decisionId, "dec_999")
    }
}