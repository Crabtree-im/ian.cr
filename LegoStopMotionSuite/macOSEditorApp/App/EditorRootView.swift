import SwiftUI

struct EditorRootView: View {
    @StateObject var viewModel: EditorViewModel

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 12) {
                Button("Start Receiver") {
                    viewModel.startReceiver()
                }

                Button("Export .mov") {
                    Task { await viewModel.exportMovie() }
                }
                .disabled(viewModel.timelineVM.frames.isEmpty)

                Button("Import Zip...") {
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

            PreviewView(currentImage: viewModel.currentPreviewImage, isPlaying: viewModel.isPlaying) {
                viewModel.togglePlayback()
            }
            .frame(minHeight: 300)
            .padding()
        }
        .task {
            viewModel.prepareProjectRoot()
        }
        .alert("Error", isPresented: $viewModel.showError) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(viewModel.errorMessage)
        }
    }
}
