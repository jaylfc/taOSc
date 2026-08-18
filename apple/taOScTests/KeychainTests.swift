import XCTest
@testable import taOSc

final class KeychainTests: XCTestCase {
    override func setUp() {
        super.setUp()
        try? KeychainStore.shared.deleteToken()
    }

    override func tearDown() {
        try? KeychainStore.shared.deleteToken()
        super.tearDown()
    }

    func testSaveAndReadToken() throws {
        let token = "test-token-123"
        try KeychainStore.shared.saveToken(token)
        let read = try KeychainStore.shared.readToken()
        XCTAssertEqual(read, token)
    }

    func testDeleteToken() throws {
        try KeychainStore.shared.saveToken("token")
        try KeychainStore.shared.deleteToken()
        let read = try KeychainStore.shared.readToken()
        XCTAssertNil(read)
    }

    func testReadTokenWhenNoneExists() throws {
        let read = try KeychainStore.shared.readToken()
        XCTAssertNil(read)
    }

    func testOverwriteToken() throws {
        try KeychainStore.shared.saveToken("first")
        try KeychainStore.shared.saveToken("second")
        let read = try KeychainStore.shared.readToken()
        XCTAssertEqual(read, "second")
    }

    func testTokenNotInUserDefaultsAfterSave() throws {
        let token = "test-token-456"
        try KeychainStore.shared.saveToken(token)
        let userDefaultsValue = UserDefaults.standard.string(forKey: "com.jaylfc.taOSc.scopedToken")
        XCTAssertNil(userDefaultsValue)
    }
}
