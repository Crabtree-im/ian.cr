import Foundation
import ZIPFoundation

public enum ArchiveUtility {
    public static func createZip(from sourceURL: URL, to zipURL: URL, keepParent: Bool = false) throws {
        if FileManager.default.fileExists(atPath: zipURL.path) {
            try FileManager.default.removeItem(at: zipURL)
        }

        try FileManager.default.zipItem(at: sourceURL, to: zipURL, shouldKeepParent: keepParent)
    }

    public static func unzip(from zipURL: URL, to destinationURL: URL) throws {
        if !FileManager.default.fileExists(atPath: destinationURL.path) {
            try FileManager.default.createDirectory(at: destinationURL, withIntermediateDirectories: true)
        }

        try FileManager.default.unzipItem(at: zipURL, to: destinationURL)
    }
}
