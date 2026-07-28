import XCTest
@testable import taOSc

final class SettingsStorageTests: XCTestCase {
    var settings: SettingsStore!

    override func setUp() {
        super.setUp()
        UserDefaults.standard.removeObject(forKey: "serverURL")
        settings = SettingsStore()
    }

    override func tearDown() {
        UserDefaults.standard.removeObject(forKey: "serverURL")
        settings = nil
        super.tearDown()
    }

    func testInitialStateHasNoURL() {
        XCTAssertFalse(settings.hasServerURL)
        XCTAssertEqual(settings.serverURL, "")
    }

    func testSavingURLPersists() {
        settings.saveServerURL("https://taos.example.com")
        XCTAssertEqual(settings.serverURL, "https://taos.example.com")
        XCTAssertTrue(settings.hasServerURL)
    }

    func testURLPersistsAcrossInstances() {
        settings.saveServerURL("https://taos.example.com")
        let newSettings = SettingsStore()
        XCTAssertEqual(newSettings.serverURL, "https://taos.example.com")
        XCTAssertTrue(newSettings.hasServerURL)
    }

    func testUpdatingURL() {
        settings.saveServerURL("https://first.example.com")
        settings.saveServerURL("https://second.example.com")
        XCTAssertEqual(settings.serverURL, "https://second.example.com")
    }

    func testClearingURL() {
        settings.saveServerURL("https://taos.example.com")
        settings.saveServerURL("")
        XCTAssertEqual(settings.serverURL, "")
        XCTAssertFalse(settings.hasServerURL)
    }

    func testUserDefaultsKeyIsCorrect() {
        settings.saveServerURL("https://taos.example.com")
        let raw = UserDefaults.standard.string(forKey: "serverURL")
        XCTAssertEqual(raw, "https://taos.example.com")
    }
}
