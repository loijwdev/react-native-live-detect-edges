import Foundation
import UIKit

/// Shared utility class for image processing operations
@objc public class LiveDetectEdgesImageProcessor: NSObject {
    
    /// Load image from URI (supports file://, content://, etc)
    @objc public static func loadImage(fromUri uri: String) -> UIImage? {
        guard let url = URL(string: uri) else {
            print("Invalid URI: \(uri)")
            return nil
        }
        
        // Handle file:// URLs
        if url.scheme == "file" {
            guard let data = try? Data(contentsOf: url),
                  let image = UIImage(data: data) else {
                print("Failed to load image from file: \(uri)")
                return nil
            }
            return image
        }
        
        // Handle content:// URLs (Android-style, convert to file path)
        if url.scheme == "content" {
            // For iOS, this shouldn't happen, but handle gracefully
            print("Content URIs not supported on iOS: \(uri)")
            return nil
        }
        
        // Handle data URLs
        if uri.hasPrefix("data:") {
            // Extract base64 data
            if let range = uri.range(of: "base64,"),
               let data = Data(base64Encoded: String(uri[range.upperBound...])),
               let image = UIImage(data: data) {
                return image
            }
            print("Failed to decode base64 image")
            return nil
        }
        
        // Try loading as file path directly
        if let image = UIImage(contentsOfFile: uri) {
            return image
        }
        
        print("Unsupported URI scheme or failed to load: \(uri)")
        return nil
    }
    
    /// Process image with perspective correction using provided quad
    public static func processImage(_ image: UIImage, withQuad quad: Quadrilateral?) -> (croppedImage: UIImage, quad: Quadrilateral?) {
        let quad = quad ?? defaultQuad(forImage: image)
        guard let ciImage = CIImage(image: image) else {
            return (image, nil)
        }
        
        let cgOrientation = CGImagePropertyOrientation(image.imageOrientation)
        let orientedImage = ciImage.oriented(forExifOrientation: Int32(cgOrientation.rawValue))
        
        // Convert quad to Cartesian coordinates for Core Image
        var cartesianScaledQuad = quad.toCartesian(withHeight: image.size.height)
        cartesianScaledQuad.reorganize()

        let filteredImage = orientedImage.applyingFilter("CIPerspectiveCorrection", parameters: [
            "inputTopLeft": CIVector(cgPoint: cartesianScaledQuad.bottomLeft),
            "inputTopRight": CIVector(cgPoint: cartesianScaledQuad.bottomRight),
            "inputBottomLeft": CIVector(cgPoint: cartesianScaledQuad.topLeft),
            "inputBottomRight": CIVector(cgPoint: cartesianScaledQuad.topRight)
        ])

        let croppedImage = UIImage.from(ciImage: filteredImage)
        return (croppedImage, quad)
    }
    
    /// Save image to temporary directory and return file URI
    @objc public static func saveImage(_ image: UIImage) -> String? {
        guard let data = image.jpegData(compressionQuality: 0.8) else { return nil }
        let fileName = UUID().uuidString + ".jpg"
        let fileURL = FileManager.default.temporaryDirectory.appendingPathComponent(fileName)
        do {
            try data.write(to: fileURL)
            return fileURL.absoluteString
        } catch {
            print("Error saving image: \(error)")
            return nil
        }
    }
    
    /// Generate default quad (90% of image size, centered)
    public static func defaultQuad(forImage image: UIImage) -> Quadrilateral {
        let topLeft = CGPoint(x: image.size.width * 0.05, y: image.size.height * 0.05)
        let topRight = CGPoint(x: image.size.width * 0.95, y: image.size.height * 0.05)
        let bottomRight = CGPoint(x: image.size.width * 0.95, y: image.size.height * 0.95)
        let bottomLeft = CGPoint(x: image.size.width * 0.05, y: image.size.height * 0.95)
        return Quadrilateral(topLeft: topLeft, topRight: topRight, bottomRight: bottomRight, bottomLeft: bottomLeft)
    }
}
