import Foundation
import Combine
import UIKit

@MainActor
final class CaptureViewModel: ObservableObject {
    private(set) var maxFrames = 50

    @Published private(set) var frameCount = 0
    @Published private(set) var isBusy = false
    @Published private(set) var isPeerConnected = false
    @Published var errorMessage = ""
    @Published var showError = false
    @Published var onionSkinEnabled = true
    @Published var onionSkinOpacity = 0.35
    @Published private(set) var onionSkinImage: UIImage?
    @Published private(set) var latestArchiveURL: URL?
    @Published private(set) var transferProgress: Double = 0
    @Published private(set) var transferStatusText = "Idle"
    @Published private(set) var canRetryUpload = false

    var isCapacityReached: Bool {
        frameCount >= maxFrames
    }

    let cameraService = CameraService()
    private let storageService = PhotoStorageService()
    private let packager = BatchPackager()
    private let transferService = MPCTransferService(mode: .sender)

    private var batchURL: URL?
    private var lastFailedArchiveURL: URL?

    init() {
        transferService.onConnectedPeersChanged = { [weak self] peers in
            Task { @MainActor in
                self?.isPeerConnected = !peers.isEmpty
            }
        }

        transferService.onSendProgress = { [weak self] progress in
            Task { @MainActor in
                self?.transferProgress = progress
            }
        }

        transferService.onTransferStateChanged = { [weak self] state in
            Task { @MainActor in
                switch state {
                case .idle:
                    self?.transferStatusText = "Idle"
                case .sending:
                    self?.transferStatusText = "Uploading..."
                    self?.canRetryUpload = false
                case .retrying(let attempt):
                    self?.transferStatusText = "Retrying (\(attempt))"
                case .success:
                    self?.transferStatusText = "Upload complete"
                    self?.transferProgress = 1.0
                    self?.canRetryUpload = false
                    self?.lastFailedArchiveURL = nil
                case .failed(let message):
                    self?.transferStatusText = "Upload failed"
                    self?.errorMessage = message
                    self?.showError = true
                    self?.canRetryUpload = true
                }
            }
        }
    }

    func onAppear() async {
        do {
            if batchURL == nil {
                batchURL = try storageService.createBatchFolder()
            }
            try await cameraService.requestAndConfigure()
            cameraService.startRunning()
            transferService.start()
            if let batchURL {
                let frameNames = try storageService.existingFrameFileNames(in: batchURL)
                frameCount = frameNames.count
                if let last = frameNames.last {
                    let lastURL = batchURL.appendingPathComponent(last)
                    if let data = try? Data(contentsOf: lastURL) {
                        onionSkinImage = UIImage(data: data)
                    }
                }
                if !frameNames.isEmpty {
                    latestArchiveURL = try createArchiveIfNeeded(in: batchURL)
                }
            }
        } catch {
            present(error)
        }
    }

    func onDisappear() {
        cameraService.stopRunning()
        transferService.stop()
    }

    func captureFrame() async {
        guard !isCapacityReached, !isBusy, let batchURL else { return }

        isBusy = true
        defer { isBusy = false }

        do {
            let jpeg = try await cameraService.captureJPEGData()
            _ = try storageService.saveNextFrame(jpegData: jpeg, to: batchURL, maxFrames: maxFrames)
            frameCount = try storageService.existingFrameFileNames(in: batchURL).count
            onionSkinImage = UIImage(data: jpeg)
            latestArchiveURL = try createArchiveIfNeeded(in: batchURL)
        } catch {
            present(error)
        }
    }

    func uploadBatch() async {
        guard !isBusy, let batchURL else { return }

        isBusy = true
        transferProgress = 0
        transferStatusText = "Preparing batch..."
        defer {
            isBusy = false
        }

        do {
            let frameNames = try storageService.existingFrameFileNames(in: batchURL)
            guard !frameNames.isEmpty else { return }

            let manifestURL = batchURL.appendingPathComponent("manifest.json")
            try packager.writeManifest(to: manifestURL, frameNames: frameNames)

            let zipURL = try createArchiveIfNeeded(in: batchURL)
            latestArchiveURL = zipURL
            lastFailedArchiveURL = zipURL
            try await transferService.sendResource(zipURL)
        } catch {
            present(error)
            transferStatusText = "Upload failed"
            canRetryUpload = true
        }
    }

    func retryUpload() async {
        guard !isBusy else { return }
        guard let archiveURL = lastFailedArchiveURL else {
            await uploadBatch()
            return
        }

        isBusy = true
        transferProgress = 0
        transferStatusText = "Retrying upload..."
        defer {
            isBusy = false
        }

        do {
            try await transferService.sendResource(archiveURL)
        } catch {
            present(error)
            transferStatusText = "Upload failed"
            canRetryUpload = true
        }
    }

    private func createArchiveIfNeeded(in batchURL: URL) throws -> URL {
        let frameNames = try storageService.existingFrameFileNames(in: batchURL)
        let manifestURL = batchURL.appendingPathComponent("manifest.json")
        try packager.writeManifest(to: manifestURL, frameNames: frameNames)
        return try packager.zipBatchFolder(batchURL)
    }

    private func present(_ error: Error) {
        errorMessage = error.localizedDescription
        showError = true
    }
}
