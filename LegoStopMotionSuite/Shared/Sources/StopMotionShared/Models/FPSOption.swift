import Foundation

public enum FPSOption: Int, CaseIterable, Identifiable, Codable, Sendable {
    case fps12 = 12
    case fps13 = 13
    case fps14 = 14
    case fps15 = 15

    public var id: Int { rawValue }

    public var frameDuration: TimeInterval {
        1.0 / TimeInterval(rawValue)
    }
}
