package com.livedetectedges

import android.content.Context
import android.graphics.PointF
import androidx.camera.core.ImageProxy
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.util.ArrayList

class DocumentDetector(private val context: Context? = null) {

    private val activeContours = ArrayList<MatOfPoint>()
    private val srcGray = Mat()
    private val srcBlur = Mat()
    private val srcBinary = Mat()
    private val srcCanny = Mat()
    private val rotatedMat = Mat()
    private val rgbMat = Mat()       // RGB frame for segmentation input
    private val enhancedMat = Mat()  // RGB frame after contrast enhancement

    // Segmentation detector (optional, falls back to Canny if segmentation is not available)
    private var segmentationDetector: SegmentationDetector? = null

    // Minimal area ratio of a candidate contour relative to the full frame.
    // Lower value makes it easier to detect smaller/partial documents while still filtering noise.
    private val AREA_THRESHOLD_RATIO = 0.02

    // Thresholds (probability levels) to try, similar to FairScan, to better handle perspective / tilted documents.
    private val SEGMENTATION_THRESHOLDS = listOf(0.3f, 0.4f, 0.5f, 0.6f, 0.7f)

    init {
        // Initialize segmentation detector if context is available
        if (context != null) {
            try {
                segmentationDetector = SegmentationDetector(context)
            } catch (e: Exception) {
                // Segmentation is not available, we will use the Canny fallback
            }
        }
    }

    fun detect(image: ImageProxy): List<PointF>? {
        // 1. Convert ImageProxy (Y plane) to grayscale Mat
        val yBuffer = image.planes[0].buffer
        val ySize = yBuffer.remaining()
        val data = ByteArray(ySize)
        yBuffer.get(data)

        // Re-allocate Mats if dimensions changed (or first run)
        if (srcGray.width() != image.width || srcGray.height() != image.height) {
            srcGray.create(image.height, image.width, CvType.CV_8UC1)
            rgbMat.create(image.height, image.width, CvType.CV_8UC3)
            enhancedMat.create(image.height, image.width, CvType.CV_8UC3)
        }
        srcGray.put(0, 0, data)

        // 2. Handle rotation from the camera sensor
        val rotation = image.imageInfo.rotationDegrees
        val processingMat = if (rotation != 0) {
            rotateMat(srcGray, rotation)
        } else {
            srcGray
        }

        // 3. Try segmentation first if available, otherwise fall back to Canny
        // Logic refactored to detectInternal for reuse with Bitmap
        return detectInternal(processingMat)
    }

    private fun findDocumentContour(contourSource: Mat, processingMat: Mat): List<PointF>? {
        // 6. Find contours (external only, similar to typical document-scanning pipelines).
        activeContours.clear()
        Imgproc.findContours(
            contourSource,
            activeContours,
            Mat(),
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
        )

        // Sort contours by area (descending) and iterate.
        val imageArea = (processingMat.width() * processingMat.height()).toDouble()
        activeContours.sortByDescending { Imgproc.contourArea(it) }

        for (contour in activeContours) {
            val area = Imgproc.contourArea(contour)
            if (area < imageArea * AREA_THRESHOLD_RATIO) continue

            // Filter out very irregular contours using bounding-box heuristics.
            val rect = Imgproc.boundingRect(contour)
            val aspectRatio = rect.width.toDouble() / rect.height.toDouble()
            val rectArea = rect.width.toDouble() * rect.height.toDouble()
            val fillRatio = area / rectArea

            // Relax filters to accept more tilted rectangles:
            // - Aspect ratio: 0.25–4.0 (instead of 0.3–3.5) to allow more extreme tilt.
            // - Fill ratio: 0.35 (instead of 0.4) to allow partially visible documents.
            if (aspectRatio < 0.25 || aspectRatio > 4.0) continue
            if (fillRatio < 0.35) continue

            val curve = MatOfPoint2f(*contour.toArray())
            val peri = Imgproc.arcLength(curve, true)
            val approx = MatOfPoint2f()
            // Use a slightly larger epsilon so that a tilted rectangle is approximated as a 4-point polygon more often.
            Imgproc.approxPolyDP(curve, approx, 0.025 * peri, true)

            if (approx.total() == 4L) {
                val points = approx.toList()
                val sortedPoints = sortPoints(points)
                return sortedPoints.map { PointF(it.x.toFloat(), it.y.toFloat()) }
            }
        }

        // No valid quad found
        return null
    }

    fun detect(bitmap: android.graphics.Bitmap): List<PointF>? {
        if (srcGray.width() != bitmap.width || srcGray.height() != bitmap.height) {
            srcGray.create(bitmap.height, bitmap.width, CvType.CV_8UC1)
            rgbMat.create(bitmap.height, bitmap.width, CvType.CV_8UC3)
            enhancedMat.create(bitmap.height, bitmap.width, CvType.CV_8UC3)
        }
        
        org.opencv.android.Utils.bitmapToMat(bitmap, rgbMat)
        Imgproc.cvtColor(rgbMat, srcGray, Imgproc.COLOR_RGB2GRAY)
        
        // No rotation needed for bitmap as we assume it's already oriented correctly or we handle it before passing here
        val processingMat = srcGray
        
        // Reuse the logic from the other detect method, but extracted to common helper if possible.
        // For now, duplicating the logic flow or we can refactor. 
        // Let's refactor the core logic into `detectInternal`.
        
        return detectInternal(processingMat)
    }

    private fun detectInternal(processingMat: Mat): List<PointF>? {
         // 3. Try segmentation first if available, otherwise fall back to Canny
        val probMask = if (segmentationDetector?.isModelLoaded() == true) {
            // Pre-processing
            Imgproc.cvtColor(processingMat, rgbMat, Imgproc.COLOR_GRAY2RGB)

            // Increase local contrast
            val labMat = Mat()
            Imgproc.cvtColor(rgbMat, labMat, Imgproc.COLOR_RGB2Lab)
            val labChannels = ArrayList<Mat>()
            Core.split(labMat, labChannels)
            val clahe = Imgproc.createCLAHE(2.0, Size(8.0, 8.0))
            clahe.apply(labChannels[0], labChannels[0])
            Core.merge(labChannels, enhancedMat)
            Imgproc.cvtColor(enhancedMat, enhancedMat, Imgproc.COLOR_Lab2RGB)

            // Cleanup temporary Mats
            labMat.release()
            labChannels.forEach { it.release() }

            val mask = segmentationDetector?.segment(enhancedMat)
            mask
        } else {
            null
        }

        // 4. If a probability mask is available...
        if (probMask != null) {
            for (threshold in SEGMENTATION_THRESHOLDS) {
                val binaryMask = Mat()
                Imgproc.threshold(probMask, binaryMask, threshold.toDouble(), 255.0, Imgproc.THRESH_BINARY)
                val uint8Mask = Mat()
                binaryMask.convertTo(uint8Mask, CvType.CV_8UC1)
                
                val cleaned = Mat()
                val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(3.0, 3.0))
                Imgproc.morphologyEx(uint8Mask, cleaned, Imgproc.MORPH_CLOSE, kernel)
                Imgproc.morphologyEx(cleaned, cleaned, Imgproc.MORPH_OPEN, kernel)

                val result = findDocumentContour(cleaned, processingMat)

                binaryMask.release()
                uint8Mask.release()
                cleaned.release()

                if (result != null) {
                    probMask.release()
                    return result
                }
            }
            probMask.release()
        }

        // 5. Fallback: classic OpenCV pipeline
        // Use Canny directly on blurred grayscale image for better edge preservation
        Imgproc.GaussianBlur(processingMat, srcBlur, Size(5.0, 5.0), 0.0)
        // Imgproc.threshold(srcBlur, srcBinary, 0.0, 255.0, Imgproc.THRESH_BINARY or Imgproc.THRESH_OTSU)
        Imgproc.Canny(srcBlur, srcCanny, 75.0, 200.0)

        return findDocumentContour(srcCanny, processingMat)
    }

    private fun rotateMat(src: Mat, rotationDegrees: Int): Mat {
        when (rotationDegrees) {
            90 -> {
                 Core.transpose(src, rotatedMat)
                 Core.flip(rotatedMat, rotatedMat, 1)
                 return rotatedMat
            }
            180 -> {
                 Core.flip(src, rotatedMat, -1)
                 return rotatedMat
            }
            270 -> {
                 Core.transpose(src, rotatedMat)
                 Core.flip(rotatedMat, rotatedMat, 0)
                 return rotatedMat
            }
        }
        return src
    }

    private fun sortPoints(points: List<Point>): List<Point> {
        // Sort the 4 points into: TopLeft, TopRight, BottomRight, BottomLeft.
        // The goal is to provide a consistent ordering for downstream consumers (e.g. perspective correction).

        // Sum/Diff method which is standard for document scanning
        val sum = points.map { it.x + it.y }
        val diff = points.map { it.y - it.x }

        val tlIndex = sum.indexOf(sum.minOrNull()!!)
        val brIndex = sum.indexOf(sum.maxOrNull()!!)
        val trIndex = diff.indexOf(diff.minOrNull()!!)
        val blIndex = diff.indexOf(diff.maxOrNull()!!)

        return listOf(points[tlIndex], points[trIndex], points[brIndex], points[blIndex])
    }

    // Helper to get image dimensions
    fun getWidth() = if(rotatedMat.empty()) srcGray.width() else rotatedMat.width()
    fun getHeight() = if(rotatedMat.empty()) srcGray.height() else rotatedMat.height()

    fun release() {
        segmentationDetector?.release()
    }
}
