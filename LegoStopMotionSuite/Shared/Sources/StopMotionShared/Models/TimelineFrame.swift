import Foundation

public struct TimelineFrame: Identifiable, Hashable, Sendable {
    public let id: UUID
    public let fileName: String
    public let index: Int
    public let startTime: TimeInterval
    public let duration: TimeInterval

    public init(fileName: String, index: Int, startTime: TimeInterval, duration: TimeInterval) {
        self.id = UUID()
        self.fileName = fileName
        self.index = index
        self.startTime = startTime
        self.duration = duration
    }
}
