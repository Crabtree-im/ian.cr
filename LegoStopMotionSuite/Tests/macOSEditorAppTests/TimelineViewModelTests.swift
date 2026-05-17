import Foundation
import XCTest
import StopMotionShared
@testable import EditorApp

@MainActor
final class TimelineViewModelTests: XCTestCase {
    func testRebuildsEqualFrameDurationsForSelectedFPS() {
        let vm = TimelineViewModel()
        vm.load(frames: ["frame_001.jpg", "frame_002.jpg", "frame_003.jpg"])
        vm.fps = .fps15

        XCTAssertEqual(vm.frames.count, 3)
        XCTAssertEqual(vm.frames[0].duration, (1.0 / 15.0), accuracy: 0.000001)
        XCTAssertEqual(vm.frames[1].startTime, (1.0 / 15.0), accuracy: 0.000001)
        XCTAssertEqual(vm.frames[2].startTime, (2.0 / 15.0), accuracy: 0.000001)
    }
}
