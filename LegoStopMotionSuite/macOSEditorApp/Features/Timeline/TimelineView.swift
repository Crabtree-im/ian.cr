import AppKit
import SwiftUI
import StopMotionShared

struct TimelineView: View {
    @ObservedObject var vm: TimelineViewModel
    let imageProvider: (String) -> NSImage?

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Picker("FPS", selection: $vm.fps) {
                Text("12 FPS").tag(FPSOption.fps12)
                Text("13 FPS").tag(FPSOption.fps13)
                Text("14 FPS").tag(FPSOption.fps14)
                Text("15 FPS").tag(FPSOption.fps15)
            }
            .pickerStyle(.segmented)

            ScrollView(.horizontal) {
                HStack(spacing: 10) {
                    ForEach(vm.frames) { frame in
                        VStack(spacing: 6) {
                            if let image = imageProvider(frame.fileName) {
                                Image(nsImage: image)
                                    .resizable()
                                    .scaledToFit()
                                    .frame(width: 120, height: 80)
                                    .clipShape(RoundedRectangle(cornerRadius: 8))
                            } else {
                                RoundedRectangle(cornerRadius: 8)
                                    .fill(Color.gray.opacity(0.2))
                                    .frame(width: 120, height: 80)
                            }

                            Text("#\(frame.index + 1)")
                                .font(.caption.bold())

                            Text(vm.markerText(for: frame))
                                .font(.caption2.monospacedDigit())
                                .foregroundStyle(.secondary)
                        }
                    }
                }
                .padding(.vertical, 6)
            }
        }
        .padding()
    }
}
