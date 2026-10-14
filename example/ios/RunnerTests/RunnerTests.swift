import Flutter
import UIKit
import XCTest

@testable import isometrik_flutter_call

// This demonstrates a simple unit test of the Swift portion of this plugin's implementation.
//
// See https://developer.apple.com/documentation/xctest for more information about using XCTest.

class RunnerTests: XCTestCase {

  func testInitialize() {
    let plugin = IsometrikFlutterCallPlugin()

    let call = FlutterMethodCall(methodName: "initialize", arguments: [:])

    let resultExpectation = expectation(description: "result block must be called.")
    plugin.handle(call) { result in
      XCTAssertNil(result)
      resultExpectation.fulfill()
    }
    waitForExpectations(timeout: 1)
  }

}
