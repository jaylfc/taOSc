import XCTest
@testable import taOSc

final class SettingsStorageTests: XCTestCase {
    var settings: SettingsManager!
    var testDefaults: UserDefaults!

    override func setUp() {
        super.setUp()
        testDefaults = UserDefaults(suiteName: "SettingsStorageTests")!
        testDefaults.removePersistentDomain(forName: "SettingsStorageTests")
        settings = SettingsManager(defaults: testDefaults)
    }

    override func tearDown() {
        testDefaults.removePersistentDomain(forName: "SettingsStorageTests")
        settings = nil
        testDefaults = nil
        super.tearDown()
    }

    func testDefaultHasNoServerURL() {
        settings.load()
        XCTAssertFalse(settings.hasServerURL)
        XCTAssertEqual(settings.serverURL, "")
    }

    func testSaveServerURL() {
        settings.save(url: "https://example.com")
        XCTAssertEqual(settings.serverURL, "https://example.com")
        XCTAssertTrue(settings.hasServerURL)
    }

    func testSaveURLPersistsAcrossLoad() {
        settings.save(url: "https://example.com")

        let newSettings = SettingsManager(defaults: testDefaults)
        newSettings.load()

        XCTAssertEqual(newSettings.serverURL, "https://example.com")
        XCTAssertTrue(newSettings.hasServerURL)
    }

    func testSaveURLNormalizes() {
        settings.save(url: "example.com")
        XCTAssertEqual(settings.serverURL, "https://example.com")
    }

    func testClearServerURL() {
        settings.save(url: "https://example.com")
        XCTAssertTrue(settings.hasServerURL)

        settings.clear()
        XCTAssertFalse(settings.hasServerURL)
        XCTAssertEqual(settings.serverURL, "")
    }

    func testUpdateServerURL() {
        settings.save(url: "https://first.com")
        XCTAssertEqual(settings.serverURL, "https://first.com")

        settings.save(url: "https://second.com")
        XCTAssertEqual(settings.serverURL, "https://second.com")
    }

    func testURLPersistsInUserDefaults() {
        settings.save(url: "https://example.com")

        let rawValue = testDefaults.string(forKey: "taOScServerURL")
        XCTAssertEqual(rawValue, "https://example.com")
    }
}
