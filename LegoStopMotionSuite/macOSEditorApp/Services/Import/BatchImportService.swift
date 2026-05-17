import Foundation
import StopMotionShared

struct ImportedBatch {
    let manifest: BatchManifest
    let frameURLs: [URL]
    let projectFolderURL: URL
}

struct BatchImportService {
    enum ImportError: Error {
        case missingManifest
        case missingFrame(String)
        case droppedFrames([Int])
        case checksumMismatch(String)
    }

    func importBatch(from zipURL: URL, into projectFolderURL: URL) throws -> ImportedBatch {
        let sourceFolder = projectFolderURL.appendingPathComponent("source", isDirectory: true)
        try FileManager.default.createDirectory(at: sourceFolder, withIntermediateDirectories: true)

        try ArchiveUtility.unzip(from: zipURL, to: sourceFolder)

        guard let manifestURL = findManifest(in: sourceFolder) else {
            throw ImportError.missingManifest
        }
        let frameRoot = manifestURL.deletingLastPathComponent()

        let manifestData = try Data(contentsOf: manifestURL)
        let manifest = try JSONCoding.decoder.decode(BatchManifest.self, from: manifestData)

        let dropped = detectDroppedFrameIndices(in: manifest.frames)
        if !dropped.isEmpty {
            throw ImportError.droppedFrames(dropped)
        }

        let frameURLs = try manifest.frames.map { name in
            let url = frameRoot.appendingPathComponent(name)
            guard FileManager.default.fileExists(atPath: url.path) else {
                throw ImportError.missingFrame(name)
            }

            if let expectedHash = manifest.checksums?[name] {
                let actualHash = try ChecksumUtility.sha256Hex(fileAt: url)
                guard actualHash == expectedHash else {
                    throw ImportError.checksumMismatch(name)
                }
            }

            return url
        }

        return ImportedBatch(manifest: manifest, frameURLs: frameURLs, projectFolderURL: projectFolderURL)
    }

    private func findManifest(in folder: URL) -> URL? {
        if let enumerator = FileManager.default.enumerator(at: folder, includingPropertiesForKeys: nil) {
            for case let fileURL as URL in enumerator where fileURL.lastPathComponent == "manifest.json" {
                return fileURL
            }
        }
        return nil
    }

    func detectDroppedFrameIndices(in frameNames: [String]) -> [Int] {
        let numbers = frameNames.compactMap { name -> Int? in
            let prefix = "frame_"
            let suffix = ".jpg"
            guard name.hasPrefix(prefix), name.hasSuffix(suffix) else { return nil }
            let start = name.index(name.startIndex, offsetBy: prefix.count)
            let end = name.index(name.endIndex, offsetBy: -suffix.count)
            return Int(name[start..<end])
        }
        .sorted()

        guard let maxNumber = numbers.last else { return [] }
        let set = Set(numbers)
        return (1...maxNumber).filter { !set.contains($0) }
    }
}

extension BatchImportService.ImportError: LocalizedError {
    var errorDescription: String? {
        switch self {
        case .missingManifest:
            return "The incoming batch is missing manifest.json."
        case .missingFrame(let fileName):
            return "Missing frame file: \(fileName)."
        case .droppedFrames(let indices):
            let list = indices.map(String.init).joined(separator: ", ")
            return "Dropped frame indices detected: \(list)."
        case .checksumMismatch(let fileName):
            return "Integrity check failed for \(fileName)."
        }
    }
}
