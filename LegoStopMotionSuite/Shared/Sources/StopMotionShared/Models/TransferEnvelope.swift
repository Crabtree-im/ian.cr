import Foundation

public struct TransferEnvelope: Codable, Sendable {
    public let batchID: UUID
    public let manifestFileName: String
    public let archiveName: String

    public init(batchID: UUID, manifestFileName: String = "manifest.json", archiveName: String) {
        self.batchID = batchID
        self.manifestFileName = manifestFileName
        self.archiveName = archiveName
    }
}
