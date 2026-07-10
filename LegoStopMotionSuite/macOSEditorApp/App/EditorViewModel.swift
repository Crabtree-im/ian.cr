import AppKit
import Foundation
import Combine
import StopMotionShared
import UniformTypeIdentifiers

@MainActor
final class EditorViewModel: ObservableObject {
    @Published var showError = false
    @Published var errorMessage = ""
    @Published var connectionStatus = "Receiver idle"
    @Published var isPlaying = false
    @Published var currentPreviewImage: NSImage?
    @Published var receiveProgress: Double = 0
    @Published var receiveStatusText = "Idle"
    @Published var canRetryImport = false
    @Published var isReceiverRunning = false

    let timelineVM = TimelineViewModel()

    private let receiver = MPCReceiverService()
    private let importer = BatchImportService()
    private let projectStore = ProjectStore()
    private let exporter = VideoExportService()

    private var importedFrameURLs: [URL] = []
    private var playbackTask: Task<Void, Never>?
    private var lastFailedZipURL: URL?

    init() {
        receiver.onConnectedPeersChanged = { [weak self] peers in
            Task { @MainActor in
                self?.connectionStatus = peers.isEmpty ? "Waiting for iPhone" : "Connected: \(peers.count)"
            }
        }

        receiver.onFileReceived = { [weak self] localURL in
            Task { @MainActor in
                await self?.handleIncomingZip(localURL)
            }
        }

        receiver.onReceiveProgress = { [weak self] progress in
            Task { @MainActor in
                self?.receiveProgress = progress
            }
        }

        receiver.onReceiveState = { [weak self] state in
            Task { @MainActor in
                self?.receiveStatusText = state
            }
        }
    }

    func prepareProjectRoot() {
        do {
            try projectStore.ensureRoot()
        } catch {
            present(error)
        }
    }

    func startReceiver() {
        guard !isReceiverRunning else { return }
        receiver.start()
        isReceiverRunning = true
        connectionStatus = "Advertising for capture app"
    }

    func stopReceiver() {
        guard isReceiverRunning else { return }
        receiver.stop()
        isReceiverRunning = false
        receiveProgress = 0
        receiveStatusText = "Idle"
        connectionStatus = "Receiver stopped"
    }

    func toggleReceiver() {
        if isReceiverRunning {
            stopReceiver()
        } else {
            startReceiver()
        }
    }

    func imageForFrame(_ fileName: String) -> NSImage? {
        guard let url = importedFrameURLs.first(where: { $0.lastPathComponent == fileName }) else {
            return nil
        }
        return NSImage(contentsOf: url)
    }

    func togglePlayback() {
        guard !importedFrameURLs.isEmpty else { return }
        isPlaying.toggle()
        if isPlaying {
            startPlaybackLoop()
        } else {
            playbackTask?.cancel()
            playbackTask = nil
        }
    }

    func stopAllBackgroundWork() {
        playbackTask?.cancel()
        playbackTask = nil
        isPlaying = false
        stopReceiver()
    }

    func exportMovie() async {
        guard !importedFrameURLs.isEmpty else { return }

        do {
            let saveURL = try projectStore.nextExportURL()
            let size = CGSize(width: 1920, height: 1080)
            try await exporter.export(
                frameURLs: importedFrameURLs,
                fps: timelineVM.fps.rawValue,
                renderSize: size,
                outputURL: saveURL
            )
            connectionStatus = "Exported: \(saveURL.lastPathComponent)"
        } catch {
            present(error)
        }
    }

    func importZipFromOpenPanel() async {
        let panel = NSOpenPanel()
        panel.canChooseDirectories = false
        panel.canChooseFiles = true
        panel.allowsMultipleSelection = true
        panel.allowedContentTypes = [UTType.zip, UTType.image]

        let response = panel.runModal()
        guard response == .OK else {
            return
        }

        let selectedURLs = panel.urls
        guard !selectedURLs.isEmpty else { return }

        let zipURLs = selectedURLs.filter { $0.pathExtension.lowercased() == "zip" }
        let imageURLs = selectedURLs.filter {
            guard let type = UTType(filenameExtension: $0.pathExtension) else { return false }
            return type.conforms(to: .image)
        }

        if zipURLs.count == 1, imageURLs.isEmpty {
            await handleIncomingZip(zipURLs[0])
            return
        }

        if !zipURLs.isEmpty {
            present(ImportSelectionError.mixedSelection)
            return
        }

        await handleIncomingImages(imageURLs)
    }

    func retryImport() async {
        guard let zipURL = lastFailedZipURL else { return }
        await handleIncomingZip(zipURL)
    }

    private func handleIncomingZip(_ zipURL: URL) async {
        do {
            receiveProgress = 0
            receiveStatusText = "Importing batch..."
            let imported = try importer.importBatch(from: zipURL, into: try projectStore.createProjectFolder())
            importedFrameURLs = imported.frameURLs
            timelineVM.load(frames: imported.manifest.frames)
            currentPreviewImage = importedFrameURLs.first.flatMap(NSImage.init(contentsOf:))
            connectionStatus = "Imported batch: \(imported.manifest.frames.count) frames"
            receiveStatusText = "Import complete"
            receiveProgress = 1
            canRetryImport = false
            lastFailedZipURL = nil
        } catch {
            lastFailedZipURL = zipURL
            canRetryImport = true
            receiveStatusText = "Import failed"
            present(error)
        }
    }

    private func handleIncomingImages(_ imageURLs: [URL]) async {
        do {
            receiveProgress = 0
            receiveStatusText = "Importing images..."
            let imported = try importer.importImageSequence(from: imageURLs, into: try projectStore.createProjectFolder())
            importedFrameURLs = imported.frameURLs
            timelineVM.load(frames: imported.manifest.frames)
            currentPreviewImage = importedFrameURLs.first.flatMap(NSImage.init(contentsOf:))
            connectionStatus = "Imported images: \(imported.manifest.frames.count) frames"
            receiveStatusText = "Import complete"
            receiveProgress = 1
            canRetryImport = false
            lastFailedZipURL = nil
        } catch {
            canRetryImport = false
            receiveStatusText = "Import failed"
            present(error)
        }
    }

    private func startPlaybackLoop() {
        playbackTask?.cancel()
        playbackTask = Task { @MainActor in
            guard !importedFrameURLs.isEmpty else { return }

            while !Task.isCancelled && isPlaying {
                for url in importedFrameURLs {
                    if Task.isCancelled || !isPlaying { return }
                    currentPreviewImage = NSImage(contentsOf: url)
                    let nanos = UInt64((1.0 / Double(timelineVM.fps.rawValue)) * 1_000_000_000)
                    try? await Task.sleep(nanoseconds: nanos)
                }
            }
        }
    }

    private func present(_ error: Error) {
        errorMessage = error.localizedDescription
        showError = true
    }
}

private enum ImportSelectionError: LocalizedError {
    case mixedSelection

    var errorDescription: String? {
        switch self {
        case .mixedSelection:
            return "Choose either one .zip batch or one/more image files, not both at once."
        }
    }
}
