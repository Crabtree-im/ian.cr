import Foundation

struct ProjectStore {
    private let fm = FileManager.default

    func ensureRoot() throws {
        let root = try rootDirectory()
        try fm.createDirectory(at: root, withIntermediateDirectories: true)
    }

    func createProjectFolder() throws -> URL {
        let folder = try rootDirectory().appendingPathComponent(UUID().uuidString, isDirectory: true)
        try fm.createDirectory(at: folder, withIntermediateDirectories: true)
        return folder
    }

    func nextExportURL() throws -> URL {
        let exportDir = try rootDirectory().appendingPathComponent("exports", isDirectory: true)
        try fm.createDirectory(at: exportDir, withIntermediateDirectories: true)

        let timestamp = ISO8601DateFormatter().string(from: Date()).replacingOccurrences(of: ":", with: "-")
        return exportDir.appendingPathComponent("lego_stopmotion_\(timestamp).mov")
    }

    private func rootDirectory() throws -> URL {
        let base = try fm.url(
            for: .applicationSupportDirectory,
            in: .userDomainMask,
            appropriateFor: nil,
            create: true
        )
        return base.appendingPathComponent("LegoStopMotionEditor", isDirectory: true)
    }
}
