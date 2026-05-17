import AppKit
import Foundation
import XCTest
@testable import EditorApp

final class VideoExportServiceTests: XCTestCase {
    func testExportsMovieFile() async throws {
        let fm = FileManager.default
        let tempRoot = fm.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
        try fm.createDirectory(at: tempRoot, withIntermediateDirectories: true)
        defer { try? fm.removeItem(at: tempRoot) }

        let frame1 = tempRoot.appendingPathComponent("frame_001.jpg")
        let frame2 = tempRoot.appendingPathComponent("frame_002.jpg")
        let output = tempRoot.appendingPathComponent("out.mov")

        try makeSolidImage(.red).writeJPEG(to: frame1)
        try makeSolidImage(.blue).writeJPEG(to: frame2)

        let exporter = VideoExportService()
        try await exporter.export(
            frameURLs: [frame1, frame2],
            fps: 12,
            renderSize: CGSize(width: 320, height: 240),
            outputURL: output
        )

        XCTAssertTrue(fm.fileExists(atPath: output.path))
        let attrs = try fm.attributesOfItem(atPath: output.path)
        let size = (attrs[.size] as? NSNumber)?.intValue ?? 0
        XCTAssertGreaterThan(size, 0)
    }

    private func makeSolidImage(_ color: NSColor, size: CGSize = CGSize(width: 64, height: 64)) -> NSImage {
        let image = NSImage(size: size)
        image.lockFocus()
        color.setFill()
        NSBezierPath(rect: NSRect(origin: .zero, size: size)).fill()
        image.unlockFocus()
        return image
    }
}

private extension NSImage {
    func writeJPEG(to url: URL) throws {
        guard
            let tiff = tiffRepresentation,
            let rep = NSBitmapImageRep(data: tiff),
            let data = rep.representation(using: .jpeg, properties: [.compressionFactor: 0.8])
        else {
            throw CocoaError(.coderInvalidValue)
        }
        try data.write(to: url)
    }
}
