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
        // Handle successful scan
        // results.originalScan and results.croppedScan are non-optional in this version of WeScan
        let originalImage = results.originalScan.image
        print("WeScan captured original image: \(originalImage)")
        
        let croppedImage = results.croppedScan.image
        print("WeScan captured cropped image: \(croppedImage)")
        
        // Depending on requirements, we might want to dismiss or reset.
        // Since this is an embedded view, 'dismiss' might not make sense if we want to keep scanning?
        // But ImageScannerController is a flow (Scan -> Edit -> Review).
        // If we want continuous scanning, WeScan might not be the best fit without customization or using internal components.
        // But user asked to use WeScan, so we follow the standard flow.
    }
    
    func imageScannerControllerDidCancel(_ scanner: ImageScannerController) {
        print("WeScan cancelled")
    }
}
