package com.livedetectedges

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.net.Uri
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object LiveDetectEdgesImageProcessor {

    fun processImage(bitmap: Bitmap, quad: Quadrilateral?): Bitmap {
        if (quad == null) {
            return bitmap
        }

        val srcMat = Mat()
        Utils.bitmapToMat(bitmap, srcMat)
        
        // Convert to float points for perspective transform
        val srcPoints = listOf(
            quad.topLeft,
            quad.topRight,
            quad.bottomRight,
            quad.bottomLeft
        ).map { Point(it.x.toDouble(), it.y.toDouble()) }

        val srcMatPoints = MatOfPoint2f(*srcPoints.toTypedArray())
        
        // Calculate destination dimensions
        // Width = max(distance(tl, tr), distance(bl, br))
        // Height = max(distance(tl, bl), distance(tr, br))
        val widthA = Math.hypot(srcPoints[1].x - srcPoints[0].x, srcPoints[1].y - srcPoints[0].y)
        val widthB = Math.hypot(srcPoints[2].x - srcPoints[3].x, srcPoints[2].y - srcPoints[3].y)
        val maxWidth = Math.max(widthA, widthB)

        val heightA = Math.hypot(srcPoints[3].x - srcPoints[0].x, srcPoints[3].y - srcPoints[0].y)
        val heightB = Math.hypot(srcPoints[2].x - srcPoints[1].x, srcPoints[2].y - srcPoints[1].y)
        val maxHeight = Math.max(heightA, heightB)

        val dstPoints = listOf(
            Point(0.0, 0.0),
            Point(maxWidth - 1, 0.0),
            Point(maxWidth - 1, maxHeight - 1),
            Point(0.0, maxHeight - 1)
        )
        val dstMatPoints = MatOfPoint2f(*dstPoints.toTypedArray())

        val perspectiveTransform = Imgproc.getPerspectiveTransform(srcMatPoints, dstMatPoints)
        val dstMat = Mat()
        
        Imgproc.warpPerspective(
            srcMat,
            dstMat,
            perspectiveTransform,
            Size(maxWidth, maxHeight)
        )

        val outBitmap = Bitmap.createBitmap(
            dstMat.cols(),
            dstMat.rows(),
            Bitmap.Config.ARGB_8888
        )
        Utils.matToBitmap(dstMat, outBitmap)
        
        // Cleanup
        srcMat.release()
        dstMat.release()
        perspectiveTransform.release()
        srcMatPoints.release()
        dstMatPoints.release()

        return outBitmap
    }

    fun saveImageToTempFile(context: Context, bitmap: Bitmap): String? {
        val filename = "${UUID.randomUUID()}.jpg"
        val file = File(context.cacheDir, filename)
        
        return try {
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
            out.close()
            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getBitmapFromUri(context: Context, uriString: String): Bitmap? {
        return try {
            val uri = Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

data class Quadrilateral(
    val topLeft: PointF,
    val topRight: PointF,
    val bottomRight: PointF,
    val bottomLeft: PointF
)
