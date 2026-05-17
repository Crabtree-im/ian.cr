import Foundation
import XCTest
import StopMotionShared
@testable import EditorApp

final class BatchImportServiceTests: XCTestCase {
    func testDetectsDroppedFrameIndices() {
        let service = BatchImportService()
        let dropped = service.detectDroppedFrameIndices(in: ["frame_001.jpg", "frame_003.jpg", "frame_004.jpg"])
        XCTAssertEqual(dropped, [2])
    }

    func testRejectsChecksumMismatch() throws {
        let service = BatchImportService()
        let fm = FileManager.default
        let tempRoot = fm.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
        let batchFolder = tempRoot.appendingPathComponent("batch", isDirectory: true)
        let zipURL = tempRoot.appendingPathComponent("batch.zip")
        let importTarget = tempRoot.appendingPathComponent("project", isDirectory: true)

        try fm.createDirectory(at: batchFolder, withIntermediateDirectories: true)
        try fm.createDirectory(at: importTarget, withIntermediateDirectories: true)
        defer { try? fm.removeItem(at: tempRoot) }

        let frameName = "frame_001.jpg"
        let frameURL = batchFolder.appendingPathComponent(frameName)
        try Data([0x01, 0x02, 0x03]).write(to: frameURL)

        let manifest = BatchManifest(
            batchID: UUID(),
            createdAt: Date(),
            fps: nil,
            frames: [frameName],
            checksums: [frameName: "not-a-real-hash"]
        )
        let manifestData = try JSONCoding.encoder.encode(manifest)
        try manifestData.write(to: batchFolder.appendingPathComponent("manifest.json"))

        try ArchiveUtility.createZip(from: batchFolder, to: zipURL)

        XCTAssertThrowsError(try service.importBatch(from: zipURL, into: importTarget)) { error in
            guard case BatchImportService.ImportError.checksumMismatch(let fileName) = error else {
                XCTFail("Unexpected import error: \(error)")
                return
            }
            XCTAssertEqual(fileName, frameName)
        }
    }
}
