package com.livedetectedges

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap

class LiveDetectEdgesModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String {
        return "LiveDetectEdgesModule"
    }

    @ReactMethod
    fun takePhoto(promise: Promise) {
        val view = LiveDetectEdgesView.activeView
        if (view == null) {
            promise.reject("NO_VIEW", "No active LiveDetectEdgesView found")
            return
        }

        view.captureImage { result ->
            if (result.hasKey("error")) {
                promise.reject("CAPTURE_FAILED", result.getString("error"))
            } else {
                promise.resolve(result)
            }
        }
    }

    @ReactMethod
    fun cropImage(params: ReadableMap, promise: Promise) {
        try {
            val imageUri = params.getString("imageUri")
            val quadMap = params.getMap("quad")

            if (imageUri == null || quadMap == null) {
                promise.reject("INVALID_PARAMS", "imageUri and quad are required")
                return
            }

            val topLeft = quadMap.getMap("topLeft")
            val topRight = quadMap.getMap("topRight")
            val bottomRight = quadMap.getMap("bottomRight")
            val bottomLeft = quadMap.getMap("bottomLeft")

            if (topLeft == null || topRight == null || bottomRight == null || bottomLeft == null) {
                promise.reject("INVALID_PARAMS", "Invalid quad points")
                return
            }

            val quad = Quadrilateral(
                android.graphics.PointF(topLeft.getDouble("x").toFloat(), topLeft.getDouble("y").toFloat()),
                android.graphics.PointF(topRight.getDouble("x").toFloat(), topRight.getDouble("y").toFloat()),
                android.graphics.PointF(bottomRight.getDouble("x").toFloat(), bottomRight.getDouble("y").toFloat()),
                android.graphics.PointF(bottomLeft.getDouble("x").toFloat(), bottomLeft.getDouble("y").toFloat())
            )

            val apiContext = reactApplicationContext
            val bitmap = LiveDetectEdgesImageProcessor.getBitmapFromUri(apiContext, imageUri)

            if (bitmap == null) {
                promise.reject("LOAD_FAILED", "Failed to load image from URI")
                return
            }

            val croppedBitmap = LiveDetectEdgesImageProcessor.processImage(bitmap, quad)
            val croppedUri = LiveDetectEdgesImageProcessor.saveImageToTempFile(apiContext, croppedBitmap)

            if (croppedUri == null) {
                promise.reject("SAVE_FAILED", "Failed to save cropped image")
                return
            }

            val result = Arguments.createMap()
            result.putString("uri", croppedUri)
            result.putInt("width", croppedBitmap.width)
            result.putInt("height", croppedBitmap.height)

            promise.resolve(result)

        } catch (e: Exception) {
            promise.reject("CROP_FAILED", "Error cropping image: ${e.message}", e)
        }
    }
}
