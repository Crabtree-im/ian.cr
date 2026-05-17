import AVFoundation
import Foundation

final class CameraService: NSObject {
    enum CameraError: Error {
        case unauthorized
        case unavailable
        case inputFailed
        case outputFailed
        case captureFailed
        case noImageData
    }

    let session = AVCaptureSession()

    private let output = AVCapturePhotoOutput()
    private let queue = DispatchQueue(label: "capture.camera.session")
    private var continuation: CheckedContinuation<Data, Error>?

    func requestAndConfigure() async throws {
        let granted = await AVCaptureDevice.requestAccess(for: .video)
        guard granted else {
            throw CameraError.unauthorized
        }
        try await configureIfNeeded()
    }

    func startRunning() {
        queue.async {
            if !self.session.isRunning {
                self.session.startRunning()
            }
        }
    }

    func stopRunning() {
        queue.async {
            if self.session.isRunning {
                self.session.stopRunning()
            }
        }
    }

    private func configureIfNeeded() async throws {
        try await withCheckedThrowingContinuation { (cont: CheckedContinuation<Void, Error>) in
            queue.async {
                if !self.session.inputs.isEmpty {
                    cont.resume()
                    return
                }

                self.session.beginConfiguration()
                self.session.sessionPreset = .photo

                guard let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back) else {
                    self.session.commitConfiguration()
                    cont.resume(throwing: CameraError.unavailable)
                    return
                }

                guard let input = try? AVCaptureDeviceInput(device: device) else {
                    self.session.commitConfiguration()
                    cont.resume(throwing: CameraError.inputFailed)
                    return
                }

                guard self.session.canAddInput(input) else {
                    self.session.commitConfiguration()
                    cont.resume(throwing: CameraError.inputFailed)
                    return
                }
                self.session.addInput(input)

                guard self.session.canAddOutput(self.output) else {
                    self.session.commitConfiguration()
                    cont.resume(throwing: CameraError.outputFailed)
                    return
                }
                self.session.addOutput(self.output)

                self.session.commitConfiguration()
                cont.resume()
            }
        }
    }

    func captureJPEGData() async throws -> Data {
        try await withCheckedThrowingContinuation { (cont: CheckedContinuation<Data, Error>) in
            queue.async {
                self.continuation = cont
                let settings = AVCapturePhotoSettings(format: [AVVideoCodecKey: AVVideoCodecType.jpeg])
                settings.isHighResolutionPhotoEnabled = false
                self.output.capturePhoto(with: settings, delegate: self)
            }
        }
    }
}

extension CameraService: AVCapturePhotoCaptureDelegate {
    func photoOutput(_ output: AVCapturePhotoOutput, didFinishProcessingPhoto photo: AVCapturePhoto, error: Error?) {
        if let error {
            continuation?.resume(throwing: error)
            continuation = nil
            return
        }

        guard let data = photo.fileDataRepresentation() else {
            continuation?.resume(throwing: CameraError.noImageData)
            continuation = nil
            return
        }

        continuation?.resume(returning: data)
        continuation = nil
    }
}
