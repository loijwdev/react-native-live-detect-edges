package com.livedetectedges

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.CvType
import org.opencv.imgproc.Imgproc
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class SegmentationDetector(private val context: Context) {

    private var interpreter: Interpreter? = null

    // Model input/output sizes are derived from the model at runtime instead of being hardcoded
    private var modelInputWidth: Int = 0
    private var modelInputHeight: Int = 0
    private var modelOutputWidth: Int = 0
    private var modelOutputHeight: Int = 0

    // Input buffer for the segmentation model (RGB float32)
    private var inputBuffer: ByteBuffer? = null

    // Output buffer for the segmentation model (single channel float32)
    private var outputBuffer: ByteBuffer? = null

    init {
        try {
            loadModel()
        } catch (e: Exception) {
            Log.e("SegmentationDetector", "Failed to load model", e)
        }
    }

    private fun loadModel() {
        try {
            val modelFile = "fairscan-segmentation-model.tflite"
            val assetManager = context.assets

            // Check if model file exists in assets
            val fileDescriptor = try {
                assetManager.openFd(modelFile)
            } catch (e: Exception) {
                Log.w("SegmentationDetector", "Model file not found in assets, using fallback detection")
                return
            }

            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

            val options = Interpreter.Options().apply {
                // 2 threads: same configuration as FairScan for stable performance
                numThreads = 2
            }

            interpreter = Interpreter(modelBuffer, options)

            // Derive input/output sizes from the model instead of hardcoding.
            // Use the output tensor shape as FairScan does: [1, H, W, C]
            val outputShape = interpreter!!.getOutputTensor(0).shape()
            val outH = outputShape[1]
            val outW = outputShape[2]

            modelInputWidth = outW
            modelInputHeight = outH
            modelOutputWidth = outW
            modelOutputHeight = outH

            // Initialize buffers based on dynamic sizes
            inputBuffer = ByteBuffer.allocateDirect(4 * modelInputWidth * modelInputHeight * 3) // RGB float32
            inputBuffer?.order(ByteOrder.nativeOrder())

            outputBuffer = ByteBuffer.allocateDirect(4 * modelOutputWidth * modelOutputHeight) // Single channel float32
            outputBuffer?.order(ByteOrder.nativeOrder())

            Log.d("SegmentationDetector", "Model loaded successfully")
        } catch (e: Exception) {
            Log.e("SegmentationDetector", "Error loading model", e)
        }
    }

    /**
     * Run segmentation on an input RGB image and return a probability mask (CV_32FC1).
     * Returns null if the model is not loaded or inference fails.
     * The document detector will try multiple thresholds on this probability mask.
     */
    fun segment(image: Mat): Mat? {
        val currentInterpreter = interpreter ?: return null
        val currentInputBuffer = inputBuffer ?: return null
        val currentOutputBuffer = outputBuffer ?: return null

        try {
            // Ensure input is RGB
            val rgbInput = if (image.channels() == 1) {
                val rgb = Mat()
                Imgproc.cvtColor(image, rgb, Imgproc.COLOR_GRAY2RGB)
                rgb
            } else {
                image
            }

            if (modelInputWidth == 0 || modelInputHeight == 0 ||
                modelOutputWidth == 0 || modelOutputHeight == 0) {
                Log.w("SegmentationDetector", "Model dimensions not initialized, skipping segmentation")
                if (rgbInput !== image) {
                    rgbInput.release()
                }
                return null
            }

            // Resize image to model input size (derived from the model)
            val resizedMat = Mat()
            Imgproc.resize(
                rgbInput,
                resizedMat,
                org.opencv.core.Size(modelInputWidth.toDouble(), modelInputHeight.toDouble())
            )

            // Convert Mat to Bitmap for easier processing
            val bitmap = Bitmap.createBitmap(resizedMat.width(), resizedMat.height(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(resizedMat, bitmap)

            // Prepare input buffer.
            // Normalize exactly like FairScan: (x - 127.5f) / 127.5f => [-1, 1]
            currentInputBuffer.rewind()
            val pixels = IntArray(modelInputWidth * modelInputHeight)
            bitmap.getPixels(pixels, 0, modelInputWidth, 0, 0, modelInputWidth, modelInputHeight)

            for (pixel in pixels) {
                val r = (((pixel shr 16) and 0xFF) - 127.5f) / 127.5f
                val g = (((pixel shr 8) and 0xFF) - 127.5f) / 127.5f
                val b = ((pixel and 0xFF) - 127.5f) / 127.5f
                currentInputBuffer.putFloat(r)
                currentInputBuffer.putFloat(g)
                currentInputBuffer.putFloat(b)
            }

            // Run inference
            currentOutputBuffer.rewind()
            currentInterpreter.run(currentInputBuffer, currentOutputBuffer)

            // Convert output buffer to probability mask (CV_32FC1)
            val maskMat = Mat(modelOutputHeight, modelOutputWidth, CvType.CV_32FC1)
            currentOutputBuffer.rewind()
            val maskData = FloatArray(modelOutputWidth * modelOutputHeight)
            currentOutputBuffer.asFloatBuffer().get(maskData)

            // Clamp probabilities to [0, 1] to match FairScan behavior
            for (i in maskData.indices) {
                val v = maskData[i]
                maskData[i] = when {
                    v < 0f -> 0f
                    v > 1f -> 1f
                    else -> v
                }
            }

            maskMat.put(0, 0, maskData)

            // Resize probability mask back to original image size (still CV_32FC1, no threshold applied here)
            val finalMask = Mat()
            Imgproc.resize(maskMat, finalMask, org.opencv.core.Size(image.width().toDouble(), image.height().toDouble()), 0.0, 0.0, Imgproc.INTER_LINEAR)

            // Cleanup
            bitmap.recycle()
            if (rgbInput !== image) {
                rgbInput.release()
            }
            resizedMat.release()
            maskMat.release()

            return finalMask
        } catch (e: Exception) {
            Log.e("SegmentationDetector", "Error during segmentation", e)
            return null
        }
    }

    fun isModelLoaded(): Boolean = interpreter != null

    fun release() {
        interpreter?.close()
        interpreter = null
        inputBuffer = null
        outputBuffer = null
    }
}
