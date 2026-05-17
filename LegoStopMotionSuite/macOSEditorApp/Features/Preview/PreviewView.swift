import AppKit
import SwiftUI

struct PreviewView: View {
    let currentImage: NSImage?
    let isPlaying: Bool
    let togglePlayback: () -> Void

    var body: some View {
        VStack(spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 14)
                    .fill(Color.black.opacity(0.85))

                if let currentImage {
                    Image(nsImage: currentImage)
                        .resizable()
                        .scaledToFit()
                        .padding(10)
                } else {
                    Text("No frames imported")
                        .foregroundStyle(.white.opacity(0.75))
                }
            }

            Button(isPlaying ? "Pause" : "Play") {
                togglePlayback()
            }
            .buttonStyle(.borderedProminent)
        }
    }
}
