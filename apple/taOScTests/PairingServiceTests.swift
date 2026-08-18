import XCTest
@testable import taOSc

final class PairingServiceTests: XCTestCase {
    func testPairRequestResponseParsing() throws {
        let json = """
        {"pair_request_id":"req-123","verify_code":"481905"}
        """.data(using: .utf8)!
        let response = try JSONDecoder().decode(PairRequestResponse.self, from: json)
        XCTAssertEqual(response.pair_request_id, "req-123")
        XCTAssertEqual(response.verify_code, "481905")
    }

    func testPairRequestPollResponsePending() throws {
        let json = """
        {"id":"req-123","status":"pending","scoped_token":null}
        """.data(using: .utf8)!
        let response = try JSONDecoder().decode(PairRequestPollResponse.self, from: json)
        XCTAssertEqual(response.requestStatus, .pending)
    }

    func testPairRequestPollResponseApproved() throws {
        let json = """
        {"id":"req-123","status":"approved","scoped_token":"token-abc"}
        """.data(using: .utf8)!
        let response = try JSONDecoder().decode(PairRequestPollResponse.self, from: json)
        XCTAssertEqual(response.requestStatus, .approved("token-abc"))
    }

    func testPairRequestPollResponseDenied() throws {
        let json = """
        {"id":"req-123","status":"denied","scoped_token":null}
        """.data(using: .utf8)!
        let response = try JSONDecoder().decode(PairRequestPollResponse.self, from: json)
        XCTAssertEqual(response.requestStatus, .denied)
    }

    func testPairRequestPollResponseExpired() throws {
        let json = """
        {"id":"req-123","status":"expired","scoped_token":null}
        """.data(using: .utf8)!
        let response = try JSONDecoder().decode(PairRequestPollResponse.self, from: json)
        XCTAssertEqual(response.requestStatus, .expired)
    }

    func testPairRequestPollResponseStatusCaseInsensitive() throws {
        let json = """
        {"id":"req-123","status":"PENDING","scoped_token":null}
        """.data(using: .utf8)!
        let response = try JSONDecoder().decode(PairRequestPollResponse.self, from: json)
        XCTAssertEqual(response.requestStatus, .pending)
    }

    func testPairRequestPollResponseUnknownStatus() throws {
        let json = """
        {"id":"req-123","status":"unknown","scoped_token":null}
        """.data(using: .utf8)!
        let response = try JSONDecoder().decode(PairRequestPollResponse.self, from: json)
        XCTAssertNil(response.requestStatus)
    }

    func testPairRequestPollResponseApprovedWithoutToken() throws {
        let json = """
        {"id":"req-123","status":"approved","scoped_token":null}
        """.data(using: .utf8)!
        let response = try JSONDecoder().decode(PairRequestPollResponse.self, from: json)
        XCTAssertNil(response.requestStatus)
    }

    func testTaOSgoJoinResponseParsing() throws {
        let json = """
        {"join_key":"key-123","login_server":"https://login.example.com","hosts":[{"handle":"alice","addr":"https://alice.example.com"}]}
        """.data(using: .utf8)!
        let response = try JSONDecoder().decode(TaOSgoJoinResponse.self, from: json)
        XCTAssertEqual(response.join_key, "key-123")
        XCTAssertEqual(response.login_server, "https://login.example.com")
        XCTAssertEqual(response.hosts.count, 1)
        XCTAssertEqual(response.hosts.first?.handle, "alice")
        XCTAssertEqual(response.hosts.first?.addr, "https://alice.example.com")
    }

    func testTaOSgoJoinResponseEmptyHosts() throws {
        let json = """
        {"join_key":"key-123","login_server":"https://login.example.com","hosts":[]}
        """.data(using: .utf8)!
        let response = try JSONDecoder().decode(TaOSgoJoinResponse.self, from: json)
        XCTAssertTrue(response.hosts.isEmpty)
    }

    func testTaOSgoJoinResponseMultipleHosts() throws {
        let json = """
        {"join_key":"key-123","login_server":"https://login.example.com","hosts":[{"handle":"alice","addr":"https://alice.example.com"},{"handle":"bob","addr":"https://bob.example.com"}]}
        """.data(using: .utf8)!
        let response = try JSONDecoder().decode(TaOSgoJoinResponse.self, from: json)
        XCTAssertEqual(response.hosts.count, 2)
        XCTAssertEqual(response.hosts[0].handle, "alice")
        XCTAssertEqual(response.hosts[1].addr, "https://bob.example.com")
    }
}
