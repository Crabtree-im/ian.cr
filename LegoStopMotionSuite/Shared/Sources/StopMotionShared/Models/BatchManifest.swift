import Foundation

public struct BatchManifest: Codable, Sendable {
    public let batchID: UUID
    public let createdAt: Date
    public var fps: Int?
    public let frames: [String]
    public let checksums: [String: String]?

    public init(batchID: UUID, createdAt: Date, fps: Int?, frames: [String], checksums: [String: String]? = nil) {
        self.batchID = batchID
        self.createdAt = createdAt
        self.fps = fps
        self.frames = frames
        self.checksums = checksums
    }
}
