import Foundation
import MultipeerConnectivity

final class MPCTransferService: NSObject {
    enum Mode {
        case sender
        case receiver
    }

    private let serviceType = "lego-stpmtn"
    private let peerID: MCPeerID
    private let mode: Mode

    private lazy var session: MCSession = {
        let s = MCSession(peer: peerID, securityIdentity: nil, encryptionPreference: .required)
        s.delegate = self
        return s
    }()

    private var browser: MCNearbyServiceBrowser?
    private var advertiser: MCNearbyServiceAdvertiser?

    enum TransferState {
        case idle
        case sending
        case retrying(Int)
        case success
        case failed(String)
    }

    enum TransferError: Error {
        case noConnectedPeers
        case sendFailed
    }

    var onConnectedPeersChanged: (([MCPeerID]) -> Void)?
    var onFileReceived: ((URL) -> Void)?
    var onSendProgress: ((Double) -> Void)?
    var onTransferStateChanged: ((TransferState) -> Void)?

    init(mode: Mode) {
        self.mode = mode
        self.peerID = MCPeerID(displayName: Host.current().localizedName ?? UUID().uuidString)
        super.init()
    }

    func start() {
        switch mode {
        case .sender:
            browser = MCNearbyServiceBrowser(peer: peerID, serviceType: serviceType)
            browser?.delegate = self
            browser?.startBrowsingForPeers()
        case .receiver:
            advertiser = MCNearbyServiceAdvertiser(peer: peerID, discoveryInfo: nil, serviceType: serviceType)
            advertiser?.delegate = self
            advertiser?.startAdvertisingPeer()
        }
    }

    func stop() {
        browser?.stopBrowsingForPeers()
        advertiser?.stopAdvertisingPeer()
        session.disconnect()
    }

    func sendResource(_ localURL: URL, maxRetries: Int = 2) async throws {
        guard let targetPeer = session.connectedPeers.first else {
            onTransferStateChanged?(.failed("No connected Mac peer."))
            throw TransferError.noConnectedPeers
        }

        for attempt in 0...maxRetries {
            onTransferStateChanged?(.sending)

            do {
                try await sendResourceOnce(localURL, to: targetPeer)
                onSendProgress?(1.0)
                onTransferStateChanged?(.success)
                return
            } catch {
                if attempt < maxRetries {
                    onTransferStateChanged?(.retrying(attempt + 1))
                    try await Task.sleep(nanoseconds: 500_000_000)
                    continue
                }
                onTransferStateChanged?(.failed(error.localizedDescription))
                throw error
            }
        }
    }

    private func sendResourceOnce(_ localURL: URL, to peer: MCPeerID) async throws {
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            var observation: NSKeyValueObservation?

            let progress = session.sendResource(at: localURL, withName: localURL.lastPathComponent, toPeer: peer) { error in
                observation?.invalidate()
                observation = nil

                if let error {
                    continuation.resume(throwing: error)
                } else {
                    continuation.resume()
                }
            }

            observation = progress.observe(\Progress.fractionCompleted, options: [.initial, .new]) { [weak self] progress, _ in
                self?.onSendProgress?(progress.fractionCompleted)
            }
        }
    }
}

extension MPCTransferService: MCNearbyServiceBrowserDelegate, MCNearbyServiceAdvertiserDelegate, MCSessionDelegate {
    func browser(_ browser: MCNearbyServiceBrowser, foundPeer peerID: MCPeerID, withDiscoveryInfo info: [String : String]?) {
        browser.invitePeer(peerID, to: session, withContext: nil, timeout: 15)
    }

    func browser(_ browser: MCNearbyServiceBrowser, lostPeer peerID: MCPeerID) {
        onConnectedPeersChanged?(session.connectedPeers)
    }

    func advertiser(_ advertiser: MCNearbyServiceAdvertiser, didReceiveInvitationFromPeer peerID: MCPeerID, withContext context: Data?, invitationHandler: @escaping (Bool, MCSession?) -> Void) {
        invitationHandler(true, session)
    }

    func session(_ session: MCSession, peer peerID: MCPeerID, didChange state: MCSessionState) {
        DispatchQueue.main.async {
            self.onConnectedPeersChanged?(session.connectedPeers)
        }
    }

    func session(_ session: MCSession, didReceive data: Data, fromPeer peerID: MCPeerID) {}

    func session(_ session: MCSession, didStartReceivingResourceWithName resourceName: String, fromPeer peerID: MCPeerID, with progress: Progress) {}

    func session(_ session: MCSession, didFinishReceivingResourceWithName resourceName: String, fromPeer peerID: MCPeerID, at localURL: URL?, withError error: Error?) {
        guard error == nil, let localURL else { return }
        DispatchQueue.main.async {
            self.onFileReceived?(localURL)
        }
    }

    func session(_ session: MCSession, didReceive stream: InputStream, withName streamName: String, fromPeer peerID: MCPeerID) {}
}
