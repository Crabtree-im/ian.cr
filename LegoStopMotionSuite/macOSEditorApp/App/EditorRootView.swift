import SwiftUI

struct EditorRootView: View {
    @StateObject var viewModel: EditorViewModel

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 12) {
                Button(viewModel.isReceiverRunning ? "Stop Receiver" : "Start Receiver") {
                    viewModel.toggleReceiver()
                }
                .buttonStyle(.borderedProminent)

                Button("Export .mov") {
                    Task { await viewModel.exportMovie() }
                }
                .disabled(viewModel.timelineVM.frames.isEmpty)

                Button("Import Photos/Zip...") {
                    Task { await viewModel.importZipFromOpenPanel() }
                }

                Spacer()

                Text(viewModel.connectionStatus)
                    .font(.footnote.monospaced())
            }
            .padding()

            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text(viewModel.receiveStatusText)
                        .font(.caption)
                    Spacer()
                    if viewModel.receiveProgress > 0, viewModel.receiveProgress < 1 {
                        Text("\(Int(viewModel.receiveProgress * 100))%")
                            .font(.caption.monospacedDigit())
                    }
                }

                ProgressView(value: viewModel.receiveProgress)

                if viewModel.canRetryImport {
                    Button("Retry Import") {
                        Task { await viewModel.retryImport() }
                    }
                    .buttonStyle(.borderedProminent)
                    .controlSize(.small)
                }
            }
            .padding(.horizontal)

            TimelineView(vm: viewModel.timelineVM, imageProvider: viewModel.imageForFrame)
                .frame(height: 210)

            PreviewView(
                currentImage: viewModel.currentPreviewImage,
                isPlaying: viewModel.isPlaying,
                canPlay: !viewModel.timelineVM.frames.isEmpty
            ) {
                viewModel.togglePlayback()
            }
            .frame(minHeight: 300)
            .padding()
        }
        .task {
            viewModel.prepareProjectRoot()
        }
        .onDisappear {
            viewModel.stopAllBackgroundWork()
        }
        .alert("Error", isPresented: $viewModel.showError) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(viewModel.errorMessage)
        }
    }
}
