import Foundation
import MultipeerConnectivity

final class MPCReceiverService: NSObject {
	private let serviceType = "lego-stpmtn"
	private let peerID: MCPeerID

	private lazy var session: MCSession = {
		let session = MCSession(peer: peerID, securityIdentity: nil, encryptionPreference: .required)
		session.delegate = self
		return session
	}()

	private var advertiser: MCNearbyServiceAdvertiser?
	private var activeProgressObservation: NSKeyValueObservation?

	var onConnectedPeersChanged: (([MCPeerID]) -> Void)?
	var onFileReceived: ((URL) -> Void)?
	var onReceiveProgress: ((Double) -> Void)?
	var onReceiveState: ((String) -> Void)?

	override init() {
		peerID = MCPeerID(displayName: Host.current().localizedName ?? UUID().uuidString)
		super.init()
	}

	func start() {
		advertiser = MCNearbyServiceAdvertiser(peer: peerID, discoveryInfo: nil, serviceType: serviceType)
		advertiser?.delegate = self
		advertiser?.startAdvertisingPeer()
	}

	func stop() {
		advertiser?.stopAdvertisingPeer()
		session.disconnect()
	}
}

extension MPCReceiverService: MCNearbyServiceAdvertiserDelegate, MCSessionDelegate {
	func advertiser(_ advertiser: MCNearbyServiceAdvertiser, didReceiveInvitationFromPeer peerID: MCPeerID, withContext context: Data?, invitationHandler: @escaping (Bool, MCSession?) -> Void) {
		invitationHandler(true, session)
	}

	func session(_ session: MCSession, peer peerID: MCPeerID, didChange state: MCSessionState) {
		DispatchQueue.main.async {
			self.onConnectedPeersChanged?(session.connectedPeers)
		}
	}

	func session(_ session: MCSession, didReceive data: Data, fromPeer peerID: MCPeerID) {}

	func session(_ session: MCSession, didStartReceivingResourceWithName resourceName: String, fromPeer peerID: MCPeerID, with progress: Progress) {
		DispatchQueue.main.async {
			self.onReceiveState?("Receiving \(resourceName)...")
		}

		activeProgressObservation?.invalidate()
		activeProgressObservation = progress.observe(\Progress.fractionCompleted, options: [.initial, .new]) { [weak self] progress, _ in
			DispatchQueue.main.async {
				self?.onReceiveProgress?(progress.fractionCompleted)
			}
		}
	}

	func session(_ session: MCSession, didFinishReceivingResourceWithName resourceName: String, fromPeer peerID: MCPeerID, at localURL: URL?, withError error: Error?) {
		activeProgressObservation?.invalidate()
		activeProgressObservation = nil

		guard error == nil, let localURL else {
			DispatchQueue.main.async {
				self.onReceiveState?("Receive failed")
			}
			return
		}
		DispatchQueue.main.async {
			self.onReceiveProgress?(1.0)
			self.onReceiveState?("Receive complete")
			self.onFileReceived?(localURL)
		}
	}

	func session(_ session: MCSession, didReceive stream: InputStream, withName streamName: String, fromPeer peerID: MCPeerID) {}
}
