import Foundation
import StopMotionShared

struct BatchPackager {
    func writeManifest(to manifestURL: URL, frameNames: [String], fps: Int? = nil) throws {
        let batchFolder = manifestURL.deletingLastPathComponent()
        var checksums: [String: String] = [:]

        for frameName in frameNames {
            let frameURL = batchFolder.appendingPathComponent(frameName)
            checksums[frameName] = try ChecksumUtility.sha256Hex(fileAt: frameURL)
        }

        let manifest = BatchManifest(
            batchID: UUID(),
            createdAt: Date(),
            fps: fps,
            frames: frameNames,
            checksums: checksums
        )
        let data = try JSONCoding.encoder.encode(manifest)
        try data.write(to: manifestURL, options: .atomic)
    }

    func zipBatchFolder(_ batchURL: URL) throws -> URL {
        let zipURL = batchURL.appendingPathExtension("zip")
        try ArchiveUtility.createZip(from: batchURL, to: zipURL)
        return zipURL
    }
}
