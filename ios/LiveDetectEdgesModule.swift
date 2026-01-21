import Foundation
import UIKit
import React

@objc(LiveDetectEdgesModule)
class LiveDetectEdgesModule: NSObject {
    
    @objc
    func cropImage(
        _ params: NSDictionary,
        resolve: @escaping RCTPromiseResolveBlock,
        reject: @escaping RCTPromiseRejectBlock
    ) {
        // Extract parameters
        guard let imageUri = params["imageUri"] as? String else {
            reject("INVALID_PARAMS", "imageUri is required", nil)
            return
        }
        
        guard let quadDict = params["quad"] as? NSDictionary else {
            reject("INVALID_PARAMS", "quad is required", nil)
            return
        }
        
        // Parse quad points
        guard let topLeft = parsePoint(quadDict["topLeft"]),
              let topRight = parsePoint(quadDict["topRight"]),
              let bottomRight = parsePoint(quadDict["bottomRight"]),
              let bottomLeft = parsePoint(quadDict["bottomLeft"]) else {
            reject("INVALID_PARAMS", "Invalid quad points format", nil)
            return
        }
        
        // Load image from URI
        guard let image = LiveDetectEdgesImageProcessor.loadImage(fromUri: imageUri) else {
            reject("IMAGE_LOAD_FAILED", "Failed to load image from URI: \(imageUri)", nil)
            return
        }
        
        // Create quad
        let quad = Quadrilateral(
            topLeft: topLeft,
            topRight: topRight,
            bottomRight: bottomRight,
            bottomLeft: bottomLeft
        )
        
        // Process image
        let result = LiveDetectEdgesImageProcessor.processImage(image, withQuad: quad)
        
        // Save cropped image
        guard let croppedUri = LiveDetectEdgesImageProcessor.saveImage(result.croppedImage) else {
            reject("IMAGE_SAVE_FAILED", "Failed to save cropped image", nil)
            return
        }
        
        // Return result
        let response: [String: Any] = [
            "uri": croppedUri,
            "width": result.croppedImage.size.width,
            "height": result.croppedImage.size.height
        ]
        
        resolve(response)
    }
    
    // Helper to parse point from dictionary
    private func parsePoint(_ dict: Any?) -> CGPoint? {
        guard let pointDict = dict as? NSDictionary,
              let x = pointDict["x"] as? Double,
              let y = pointDict["y"] as? Double else {
            return nil
        }
        return CGPoint(x: x, y: y)
    }
    
    @objc
    static func requiresMainQueueSetup() -> Bool {
        return false
    }
}
