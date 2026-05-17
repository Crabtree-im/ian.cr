import Foundation
import StopMotionShared

struct PhotoStorageService {
    enum StorageError: Error {
        case maxFramesReached
    }

    func createBatchFolder() throws -> URL {
        let base = try appSupportRoot()
        let batchURL = base.appendingPathComponent("batches", isDirectory: true)
            .appendingPathComponent(UUID().uuidString, isDirectory: true)
        try FileManager.default.createDirectory(at: batchURL, withIntermediateDirectories: true)
        return batchURL
    }

    func existingFrameFileNames(in batchURL: URL) throws -> [String] {
        let urls = try FileManager.default.contentsOfDirectory(at: batchURL, includingPropertiesForKeys: nil)
            .filter { $0.pathExtension.lowercased() == "jpg" }
            .sorted { $0.lastPathComponent < $1.lastPathComponent }
        return urls.map(\.lastPathComponent)
    }

    @discardableResult
    func saveNextFrame(jpegData: Data, to batchURL: URL, maxFrames: Int) throws -> String {
        let current = try existingFrameFileNames(in: batchURL)
        guard current.count < maxFrames else {
            throw StorageError.maxFramesReached
        }

        let name = FileNaming.frameName(index: current.count + 1)
        let url = batchURL.appendingPathComponent(name)
        try jpegData.write(to: url, options: .atomic)
        return name
    }

    private func appSupportRoot() throws -> URL {
        let base = try FileManager.default.url(
            for: .applicationSupportDirectory,
            in: .userDomainMask,
            appropriateFor: nil,
            create: true
        )
        let root = base.appendingPathComponent("LegoStopMotion", isDirectory: true)
        try FileManager.default.createDirectory(at: root, withIntermediateDirectories: true)
        return root
    }
}
