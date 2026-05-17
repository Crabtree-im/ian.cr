import Foundation

public enum ArchiveUtility {
    public enum ArchiveError: Error {
        case commandFailed(String)
    }

    public static func createZip(from sourceURL: URL, to zipURL: URL, keepParent: Bool = false) throws {
        if FileManager.default.fileExists(atPath: zipURL.path) {
            try FileManager.default.removeItem(at: zipURL)
        }

        var args = ["-c", "-k", "--sequesterRsrc"]
        if keepParent {
            args.append("--keepParent")
        }
        args.append(sourceURL.path)
        args.append(zipURL.path)

        try runDitto(arguments: args)
    }

    public static func unzip(from zipURL: URL, to destinationURL: URL) throws {
        if !FileManager.default.fileExists(atPath: destinationURL.path) {
            try FileManager.default.createDirectory(at: destinationURL, withIntermediateDirectories: true)
        }

        try runDitto(arguments: ["-x", "-k", zipURL.path, destinationURL.path])
    }

    private static func runDitto(arguments: [String]) throws {
        let process = Process()
        process.executableURL = URL(fileURLWithPath: "/usr/bin/ditto")
        process.arguments = arguments

        let stderr = Pipe()
        process.standardError = stderr

        try process.run()
        process.waitUntilExit()

        guard process.terminationStatus == 0 else {
            let data = stderr.fileHandleForReading.readDataToEndOfFile()
            let message = String(data: data, encoding: .utf8) ?? "Unknown archive error"
            throw ArchiveError.commandFailed(message)
        }
    }
}
