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
        receiver.start()
        connectionStatus = "Advertising for capture app"
    }

    func imageForFrame(_ fileName: String) -> NSImage? {
        guard let url = importedFrameURLs.first(where: { $0.lastPathComponent == fileName }) else {
            return nil
        }
        return NSImage(contentsOf: url)
    }

    func togglePlayback() {
        isPlaying.toggle()
        if isPlaying {
            startPlaybackLoop()
        } else {
            playbackTask?.cancel()
            playbackTask = nil
        }
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
        panel.allowsMultipleSelection = false
        panel.allowedContentTypes = [UTType.zip]

        let response = panel.runModal()
        guard response == .OK, let selectedURL = panel.url else {
            return
        }

        await handleIncomingZip(selectedURL)
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
