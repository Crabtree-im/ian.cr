import SwiftUI

struct CaptureView: View {
    @StateObject var viewModel: CaptureViewModel

    var body: some View {
        ZStack(alignment: .bottom) {
            CameraPreviewView(session: viewModel.cameraService.session)
                .ignoresSafeArea()

            if viewModel.onionSkinEnabled, let onionImage = viewModel.onionSkinImage {
                Image(uiImage: onionImage)
                    .resizable()
                    .scaledToFill()
                    .ignoresSafeArea()
                    .opacity(viewModel.onionSkinOpacity)
                    .allowsHitTesting(false)
            }

            VStack(spacing: 12) {
                if viewModel.isCapacityReached {
                    Text("Photo capacity reached.")
                        .font(.headline)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 10)
                        .background(.ultraThinMaterial)
                        .clipShape(Capsule())
                }

                HStack(spacing: 16) {
                    Button {
                        Task { await viewModel.captureFrame() }
                    } label: {
                        Text(viewModel.isCapacityReached ? "Maxed" : "Capture")
                            .fontWeight(.semibold)
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(viewModel.isCapacityReached || viewModel.isBusy)

                    Button {
                        Task { await viewModel.uploadBatch() }
                    } label: {
                        Text("Upload")
                            .fontWeight(.semibold)
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                    .disabled(viewModel.frameCount == 0 || viewModel.isBusy || !viewModel.isPeerConnected)

                    if let archiveURL = viewModel.latestArchiveURL {
                        ShareLink(item: archiveURL) {
                            Text("AirDrop")
                                .fontWeight(.semibold)
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.bordered)
                        .disabled(viewModel.isBusy)
                    } else {
                        Button("AirDrop") {}
                            .buttonStyle(.bordered)
                            .disabled(true)
                    }
                }

                HStack(spacing: 10) {
                    Toggle("Onion Skin", isOn: $viewModel.onionSkinEnabled)
                        .toggleStyle(.switch)

                    Slider(value: $viewModel.onionSkinOpacity, in: 0.1...0.8)
                        .disabled(!viewModel.onionSkinEnabled)
                }

                VStack(alignment: .leading, spacing: 6) {
                    HStack {
                        Text(viewModel.transferStatusText)
                            .font(.caption)
                        Spacer()
                        if viewModel.transferProgress > 0, viewModel.transferProgress < 1 {
                            Text("\(Int(viewModel.transferProgress * 100))%")
                                .font(.caption.monospacedDigit())
                        }
                    }

                    ProgressView(value: viewModel.transferProgress)

                    if viewModel.canRetryUpload {
                        Button("Retry Upload") {
                            Task { await viewModel.retryUpload() }
                        }
                        .buttonStyle(.borderedProminent)
                        .controlSize(.small)
                    }
                }

                HStack {
                    Text("Frames: \(viewModel.frameCount)/\(viewModel.maxFrames)")
                    Spacer()
                    Text(viewModel.isPeerConnected ? "Mac Connected" : "Waiting for Mac")
                }
                .font(.footnote.monospacedDigit())
                .padding(.horizontal)
                .padding(.bottom, 4)
            }
            .padding()
            .background(.regularMaterial)
        }
        .task { await viewModel.onAppear() }
        .onDisappear {
            viewModel.onDisappear()
        }
        .alert("Error", isPresented: $viewModel.showError) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(viewModel.errorMessage)
        }
    }
}
