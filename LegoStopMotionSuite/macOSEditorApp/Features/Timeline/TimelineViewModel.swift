import Foundation
import StopMotionShared

@MainActor
final class TimelineViewModel: ObservableObject {
    @Published var fps: FPSOption = .fps12 {
        didSet { rebuild() }
    }

    @Published private(set) var frames: [TimelineFrame] = []

    private var frameNames: [String] = []

    func load(frames frameNames: [String]) {
        self.frameNames = frameNames
        rebuild()
    }

    private func rebuild() {
        let duration = fps.frameDuration
        frames = frameNames.enumerated().map { idx, name in
            TimelineFrame(
                fileName: name,
                index: idx,
                startTime: TimeInterval(idx) * duration,
                duration: duration
            )
        }
    }

    func markerText(for frame: TimelineFrame) -> String {
        String(format: "%.2fs", frame.startTime)
    }
}
