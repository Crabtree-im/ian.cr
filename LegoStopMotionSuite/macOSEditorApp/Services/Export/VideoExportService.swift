import AppKit
import AVFoundation
import CoreVideo
import Foundation

struct VideoExportService {
    enum ExportError: Error {
        case cannotCreateWriter
        case cannotAddInput
        case cannotCreatePixelBuffer
        case appendFailed
    }

    func export(frameURLs: [URL], fps: Int, renderSize: CGSize, outputURL: URL) async throws {
        if FileManager.default.fileExists(atPath: outputURL.path) {
            try FileManager.default.removeItem(at: outputURL)
        }

        guard let writer = try? AVAssetWriter(outputURL: outputURL, fileType: .mov) else {
            throw ExportError.cannotCreateWriter
        }

        let settings: [String: Any] = [
            AVVideoCodecKey: AVVideoCodecType.h264,
            AVVideoWidthKey: Int(renderSize.width),
            AVVideoHeightKey: Int(renderSize.height)
        ]

        let writerInput = AVAssetWriterInput(mediaType: .video, outputSettings: settings)
        writerInput.expectsMediaDataInRealTime = false

        let attrs: [String: Any] = [
            kCVPixelBufferPixelFormatTypeKey as String: Int(kCVPixelFormatType_32BGRA),
            kCVPixelBufferWidthKey as String: Int(renderSize.width),
            kCVPixelBufferHeightKey as String: Int(renderSize.height)
        ]

        let adaptor = AVAssetWriterInputPixelBufferAdaptor(
            assetWriterInput: writerInput,
            sourcePixelBufferAttributes: attrs
        )

        guard writer.canAdd(writerInput) else {
            throw ExportError.cannotAddInput
        }
        writer.add(writerInput)

        writer.startWriting()
        writer.startSession(atSourceTime: .zero)

        let frameDuration = CMTime(value: 1, timescale: CMTimeScale(fps))

        for (index, url) in frameURLs.enumerated() {
            while !writerInput.isReadyForMoreMediaData {
                try await Task.sleep(nanoseconds: 2_000_000)
            }

            guard
                let image = NSImage(contentsOf: url),
                let buffer = makePixelBuffer(from: image, size: renderSize)
            else {
                throw ExportError.cannotCreatePixelBuffer
            }

            let time = CMTimeMultiply(frameDuration, multiplier: Int32(index))
            if !adaptor.append(buffer, withPresentationTime: time) {
                throw ExportError.appendFailed
            }
        }

        writerInput.markAsFinished()

        try await withCheckedThrowingContinuation { (cont: CheckedContinuation<Void, Error>) in
            writer.finishWriting {
                if let error = writer.error {
                    cont.resume(throwing: error)
                } else {
                    cont.resume()
                }
            }
        }
    }

    private func makePixelBuffer(from image: NSImage, size: CGSize) -> CVPixelBuffer? {
        var pixelBuffer: CVPixelBuffer?
        let options = [
            kCVPixelBufferCGImageCompatibilityKey: true,
            kCVPixelBufferCGBitmapContextCompatibilityKey: true
        ] as CFDictionary

        CVPixelBufferCreate(
            kCFAllocatorDefault,
            Int(size.width),
            Int(size.height),
            kCVPixelFormatType_32BGRA,
            options,
            &pixelBuffer
        )

        guard let buffer = pixelBuffer else { return nil }
        CVPixelBufferLockBaseAddress(buffer, [])
        defer { CVPixelBufferUnlockBaseAddress(buffer, []) }

        guard
            let context = CGContext(
                data: CVPixelBufferGetBaseAddress(buffer),
                width: Int(size.width),
                height: Int(size.height),
                bitsPerComponent: 8,
                bytesPerRow: CVPixelBufferGetBytesPerRow(buffer),
                space: CGColorSpaceCreateDeviceRGB(),
                bitmapInfo: CGImageAlphaInfo.premultipliedFirst.rawValue
            )
        else {
            return nil
        }

        let rect = CGRect(origin: .zero, size: size)
        context.setFillColor(NSColor.black.cgColor)
        context.fill(rect)

        guard let cgImage = image.cgImage(forProposedRect: nil, context: nil, hints: nil) else {
            return nil
        }
        context.draw(cgImage, in: rect)

        return buffer
    }
}
