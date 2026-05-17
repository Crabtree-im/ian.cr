import CryptoKit
import Foundation

public enum ChecksumUtility {
    public static func sha256Hex(data: Data) -> String {
        let digest = SHA256.hash(data: data)
        return digest.map { String(format: "%02x", $0) }.joined()
    }

    public static func sha256Hex(fileAt url: URL) throws -> String {
        let data = try Data(contentsOf: url)
        return sha256Hex(data: data)
    }
}
