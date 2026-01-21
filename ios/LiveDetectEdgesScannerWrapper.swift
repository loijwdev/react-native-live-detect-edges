import Foundation
import UIKit


@objc public class LiveDetectEdgesScannerWrapper: UIView {
    private var scannerController: ScannerViewController?
    private var delegateHandler: ScannerDelegateHandler?

    @objc public var overlayColor: UIColor? {
        didSet {
            scannerController?.overlayColor = overlayColor
        }
    }
    
    @objc public var overlayFillColor: UIColor? {
        didSet {
            scannerController?.overlayFillColor = overlayFillColor
        }
    }
    
    @objc public var overlayStrokeWidth: CGFloat = 1.0 {
        didSet {
            scannerController?.overlayStrokeWidth = overlayStrokeWidth
        }
    }

    @objc public override init(frame: CGRect) {
        super.init(frame: frame)
        setupScanner()
    }

    @objc public required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupScanner()
    }

    private func setupScanner() {
        // Register as active wrapper
        LiveDetectEdgesModule.activeWrapper = self
        
        let handler = ScannerDelegateHandler()
        self.delegateHandler = handler
        
        // Use ScannerViewController directly for custom UI (no buttons)
        let scanner = ScannerViewController()
        
        // We need to set the delegate if ScannerViewController exposes one.
        // Looking at ScannerViewController source, it doesn't have a public delegate property easily accessible 
        // that matches ImageScannerControllerDelegate. 
        // It uses CaptureSessionManager internally and acts as a delegate to it.
        // It pushes EditScanViewController on capture.
        
        // However, we want to intercept the results.
        // ScannerViewController doesn't seem to expose a delegate property for external use easily in its original form.
        // But since we vendored it, we can check if we can access the results.
        
        self.scannerController = scanner
        
        if let scannerView = scanner.view {
            scannerView.frame = self.bounds
            scannerView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
            self.addSubview(scannerView)
            
            // Trigger view lifecycle
            // In a real app we might want to parent the VC, but for now just adding view.
        }
    }
    
    @objc public override func didMoveToWindow() {
        super.didMoveToWindow()
        // If we needed to parent the VC, we would traverse responder chain here.
    }
}

// Private delegate handler to avoid exposing WeScan types to Objective-C
private class ScannerDelegateHandler: NSObject, ImageScannerControllerDelegate {
    
    func imageScannerController(_ scanner: ImageScannerController, didFailWithError error: Error) {
        print("WeScan failed with error: \(error)")
    }
    
    func imageScannerController(_ scanner: ImageScannerController, didFinishScanningWithResults results: ImageScannerResults) {
        // This delegate method is used when the full WeScan flow is used (which we might not be using fully here)
        // Since we are using ScannerViewController directly and intercepting via onCapture, this might be redundant
        // but kept for completeness if we ever switch modes.
    }
    
    func imageScannerControllerDidCancel(_ scanner: ImageScannerController) {
        print("WeScan cancelled")
    }
}

extension LiveDetectEdgesScannerWrapper {
    @objc public func captureImage(completion: @escaping ([String: Any]) -> Void) {
        guard let scanner = self.scannerController else {
            completion(["error": "Scanner not initialized"])
            return
        }
        
        scanner.onCapture = { [weak self] image, quad in
            guard let self = self else { return }
            
            // 1. Process Image
            let processingResult = self.processImage(image, withQuad: quad)
            
            // 2. Save Images
            let originalUri = self.saveImage(image)
            let croppedUri = self.saveImage(processingResult.croppedImage)
            
            // 3. Prepare Response
            var detectedPoints: [[String: Double]] = []
            if let quad = processingResult.quad {
                 // Return points in original image coordinates
                detectedPoints = [
                    ["x": quad.topLeft.x, "y": quad.topLeft.y],
                    ["x": quad.topRight.x, "y": quad.topRight.y],
                    ["x": quad.bottomRight.x, "y": quad.bottomRight.y],
                    ["x": quad.bottomLeft.x, "y": quad.bottomLeft.y]
                ]
            }
            
            let response: [String: Any] = [
                "image": [
                    "uri": croppedUri ?? "",
                    "width": processingResult.croppedImage.size.width,
                    "height": processingResult.croppedImage.size.height
                ],
                "originalImage": [
                    "uri": originalUri ?? "",
                    "width": image.size.width,
                    "height": image.size.height
                ],
                "detectedPoints": detectedPoints
            ]
            
            completion(response)
            
            // Reset handler to avoid retain cycles or unexpected behavior if reused
            scanner.onCapture = nil
        }
        
        scanner.capture()
    }
    
    private func processImage(_ image: UIImage, withQuad quad: Quadrilateral?) -> (croppedImage: UIImage, quad: Quadrilateral?) {
        return LiveDetectEdgesImageProcessor.processImage(image, withQuad: quad)
    }
    
    private func saveImage(_ image: UIImage) -> String? {
        return LiveDetectEdgesImageProcessor.saveImage(image)
    }
}
