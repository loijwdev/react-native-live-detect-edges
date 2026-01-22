package com.livedetectedges

import android.content.Context
import android.graphics.PointF
import android.util.Log
import android.view.Surface
import android.widget.FrameLayout
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.facebook.react.bridge.LifecycleEventListener
import com.facebook.react.bridge.ReactContext
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executors
import kotlin.math.max

class LiveDetectEdgesView(context: Context) : FrameLayout(context), LifecycleEventListener {

    companion object {
        var activeView: LiveDetectEdgesView? = null
    }

    private val previewView: PreviewView = PreviewView(context)
    private val overlayView: OverlayView = OverlayView(context)
    private var detector: DocumentDetector? = null
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var isCameraStarted = false

    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    private var imageCapture: ImageCapture? = null

    init {
        if (org.opencv.android.OpenCVLoader.initDebug()) {
            Log.d("LiveDetectEdgesView", "OpenCV initialization success!")
            detector = DocumentDetector(context)
        } else {
            Log.e("LiveDetectEdgesView", "OpenCV initialization failed!")
        }

        // TextureView mode is usually more stable for React Native
        previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE

        addView(previewView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        addView(overlayView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        previewView.scaleType = PreviewView.ScaleType.FILL_CENTER

        if (context is ReactContext) {
            context.addLifecycleEventListener(this)
        }
    }

    // Fix black screen issue: Force layout of child views
    override fun requestLayout() {
        super.requestLayout()
        post(measureAndLayout)
    }

    private val measureAndLayout = Runnable {
        measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        )
        layout(left, top, right, bottom)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d("LiveDetectEdgesView", "onAttachedToWindow")
        activeView = this
        startCamera()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Log.d("LiveDetectEdgesView", "onDetachedFromWindow")
        stopCamera()
        if (activeView == this) {
            activeView = null
        }
    }

    private fun startCamera() {
        if (isCameraStarted) return

        val reactContext = context as? ReactContext ?: return
        val activity = reactContext.currentActivity as? LifecycleOwner

        if (activity == null || activity.lifecycle.currentState == Lifecycle.State.DESTROYED) {
            Log.e("LiveDetectEdgesView", "Activity is null or destroyed, cannot start camera")
            return
        }

        cameraProviderFuture = ProcessCameraProvider.getInstance(reactContext)
        cameraProviderFuture?.addListener({
            try {
                if (!isAttachedToWindow) return@addListener

                val provider = cameraProviderFuture?.get()
                cameraProvider = provider
                if (provider != null) {
                    bindCameraUseCases(provider, activity)
                    isCameraStarted = true
                }
            } catch (e: Exception) {
                Log.e("LiveDetectEdgesView", "Failed to get camera provider", e)
            }
        }, ContextCompat.getMainExecutor(reactContext))
    }

    private fun stopCamera() {
        isCameraStarted = false
        try {
            cameraProviderFuture?.cancel(true)
            cameraProvider?.unbindAll()
            cameraProvider = null
        } catch (e: Exception) {
            Log.e("LiveDetectEdgesView", "Error stopping camera", e)
        }
    }

    private fun bindCameraUseCases(cameraProvider: ProcessCameraProvider, lifecycleOwner: LifecycleOwner) {
        if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) return

        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        val targetRotation = previewView.display?.rotation ?: Surface.ROTATION_0

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(targetRotation)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImage(imageProxy)
                }
            }
        
        imageCapture = ImageCapture.Builder()
            .setTargetRotation(targetRotation)
            .build()

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis,
                imageCapture
            )
            // Trigger redraw after successful bind
            requestLayout()
        } catch (exc: Exception) {
            Log.e("LiveDetectEdgesView", "Use case binding failed", exc)
        }
    }

    private fun processImage(imageProxy: ImageProxy) {
        val currentDetector = detector
        if (currentDetector == null) {
            imageProxy.close()
            return
        }

        try {
            val detectedPoints = currentDetector.detect(imageProxy)
            if (detectedPoints != null) {
                val mappedPoints = mapPoints(detectedPoints, currentDetector.getWidth(), currentDetector.getHeight())
                overlayView.updatePoints(mappedPoints)
            } else {
                overlayView.updatePoints(null)
            }
        } catch (e: Exception) {
            Log.e("LiveDetectEdgesView", "Error processing image", e)
        } finally {
            imageProxy.close()
        }
    }

    private fun mapPoints(points: List<PointF>, imageWidth: Int, imageHeight: Int): List<PointF> {
        val viewWidth_f = width.toFloat()
        val viewHeight_f = height.toFloat()

        if (viewWidth_f <= 0f || viewHeight_f <= 0f || imageWidth <= 0 || imageHeight <= 0) return points

        val scale = max(viewWidth_f / imageWidth, viewHeight_f / imageHeight)
        val scaledWidth = imageWidth * scale
        val scaledHeight = imageHeight * scale

        val dx = (viewWidth_f - scaledWidth) / 2
        val dy = (viewHeight_f - scaledHeight) / 2

        return points.map { p ->
            PointF(p.x * scale + dx, p.y * scale + dy)
        }
    }

    override fun onHostResume() {
        startCamera()
    }

    override fun onHostPause() {}

    override fun onHostDestroy() {
        stopCamera()
        detector?.release()
        cameraExecutor.shutdown()
    }

    fun setOverlayColor(color: Int) = overlayView.setOverlayColor(color)
    fun setOverlayFillColor(color: Int?) = overlayView.setOverlayFillColor(color)
    fun setOverlayStrokeWidth(width: Float) = overlayView.setOverlayStrokeWidth(width)

    fun captureImage(callback: (WritableMap) -> Unit) {
        val imageCapture = imageCapture ?: return
        
        val context = context
        
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onError(exc: ImageCaptureException) {
                    val map = Arguments.createMap()
                    map.putString("error", "Capture failed: ${exc.message}")
                    callback(map)
                }

                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    // Process on background thread to avoid blocking UI
                    cameraExecutor.execute {
                        try {
                            val bitmap = imageProxy.toBitmap()
                            // Correct rotation locally if needed, but ImageCapture usually handles it or provides rotation degrees
                            // For simplicity, we assume toBitmap handles conversion well, but we might need rotation
                            val rotation = imageProxy.imageInfo.rotationDegrees
                            val correctedBitmap = if(rotation != 0) {
                                val matrix = android.graphics.Matrix()
                                matrix.postRotate(rotation.toFloat())
                                android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                            } else {
                                bitmap
                            }

                            // 1. Detect edges on the captured high-res image
                            val detectedPoints = detector?.detect(correctedBitmap)
                            val quad = if (detectedPoints != null && detectedPoints.size == 4) {
                                Quadrilateral(detectedPoints[0], detectedPoints[1], detectedPoints[2], detectedPoints[3])
                            } else {
                                null
                            }

                            // 2. Process image (perspective crop)
                            val croppedBitmap = LiveDetectEdgesImageProcessor.processImage(correctedBitmap, quad)

                            // 3. Save images
                            val originalUri = LiveDetectEdgesImageProcessor.saveImageToTempFile(context, correctedBitmap)
                            val croppedUri = LiveDetectEdgesImageProcessor.saveImageToTempFile(context, croppedBitmap)

                            // 4. Prepare response
                            val response = Arguments.createMap()
                            
                            val imageMap = Arguments.createMap()
                            imageMap.putString("uri", croppedUri)
                            imageMap.putInt("width", croppedBitmap.width)
                            imageMap.putInt("height", croppedBitmap.height)
                            response.putMap("image", imageMap)

                            val originalImageMap = Arguments.createMap()
                            originalImageMap.putString("uri", originalUri)
                            originalImageMap.putInt("width", correctedBitmap.width)
                            originalImageMap.putInt("height", correctedBitmap.height)
                            response.putMap("originalImage", originalImageMap)

                            if (quad != null) {
                                val pointsArray = Arguments.createArray()
                                // Order: TL, TR, BR, BL
                                val pointsList = listOf(quad.topLeft, quad.topRight, quad.bottomRight, quad.bottomLeft)
                                for (p in pointsList) {
                                    val pointMap = Arguments.createMap()
                                    pointMap.putDouble("x", p.x.toDouble())
                                    pointMap.putDouble("y", p.y.toDouble())
                                    pointsArray.pushMap(pointMap)
                                }
                                response.putArray("detectedPoints", pointsArray)
                            } else {
                                response.putArray("detectedPoints", Arguments.createArray())
                            }

                            // Callback on main thread
                            ContextCompat.getMainExecutor(context).execute {
                                callback(response)
                            }
                            
                            // Cleanup bitmaps could be done here or rely on GC. 
                            // imageProxy is closed automatically by super or we should close it?
                            // Docs say: onCaptureSuccess(image: ImageProxy) - The application is responsible for closing the ImageProxy
                            imageProxy.close()

                        } catch (e: Exception) {
                            Log.e("LiveDetectEdgesView", "Error processing captured image", e)
                            val map = Arguments.createMap()
                            map.putString("error", "Error processing image: ${e.message}")
                            ContextCompat.getMainExecutor(context).execute {
                                callback(map)
                            }
                            imageProxy.close()
                        }
                    }
                }
            }
        )
    }

}
