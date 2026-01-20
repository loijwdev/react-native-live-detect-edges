package com.livedetectedges

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.AttributeSet
import android.view.Choreographer
import android.view.View
import kotlin.math.exp
import kotlin.math.sqrt

class OverlayView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

  private val strokePaint = Paint().apply {
    color = Color.GREEN
    strokeWidth = 5f
    style = Paint.Style.STROKE
    strokeJoin = Paint.Join.ROUND
    strokeCap = Paint.Cap.ROUND
    isAntiAlias = true
  }

  private val fillPaint = Paint().apply {
    color = Color.argb(60, 0, 255, 0)
    style = Paint.Style.FILL
    isAntiAlias = true
  }

  private var customFillColor: Int? = null // Custom fill color set from RN, null means auto-generate from stroke color

  // Reuse objects to avoid memory allocations during animation
  private val currentPoints = List(4) { PointF() }
  private var targetPoints: List<PointF>? = null
  private var hasFirstPoints = false

  private val path = Path()
  private val choreographer = Choreographer.getInstance()
  private var isAnimating = false
  private var lastFrameTimeNanos: Long = 0

  // Smoothness constant: Higher = faster/snappier, Lower = smoother/slower
  // 15.0f provides a very fluid, "liquid" movement
  private val interpolationSpeed = 15.0f

  private var missedFrames = 0

  fun updatePoints(newPoints: List<PointF>?) {
    if (newPoints != null && newPoints.size == 4) {
      missedFrames = 0
      targetPoints = newPoints

      if (!hasFirstPoints) {
        // Initialize current points with the first detection
        for (i in 0..3) {
          currentPoints[i].set(newPoints[i])
        }
        hasFirstPoints = true
        invalidate()
      }
      startAnimation()
    } else {
      targetPoints = null
      hasFirstPoints = false
      stopAnimation()
      invalidate()
    }
  }

  private val frameCallback = object : Choreographer.FrameCallback {
    override fun doFrame(frameTimeNanos: Long) {
      if (!isAnimating || targetPoints == null) return

      if (lastFrameTimeNanos == 0L) {
        lastFrameTimeNanos = frameTimeNanos
        choreographer.postFrameCallback(this)
        return
      }

      val deltaTime = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f
      lastFrameTimeNanos = frameTimeNanos

      // Time-independent lerp factor: 1 - exp(-speed * dt)
      val t = 1f - exp(-interpolationSpeed * deltaTime)

      var maxChange = 0f
      targetPoints?.let { targets ->
        for (i in 0..3) {
          val target = targets[i]
          val current = currentPoints[i]

          val dx = target.x - current.x
          val dy = target.y - current.y

          current.x += dx * t
          current.y += dy * t

          maxChange = maxOf(maxChange, sqrt(dx * dx + dy * dy))
        }
      }

      invalidate()

      // Continue animating if we haven't reached the target or if we have active targets
      if (isAnimating) {
        choreographer.postFrameCallback(this)
      }
    }
  }

  private fun startAnimation() {
    if (isAnimating) return
    isAnimating = true
    lastFrameTimeNanos = 0
    choreographer.postFrameCallback(frameCallback)
  }

  private fun stopAnimation() {
    isAnimating = false
    lastFrameTimeNanos = 0
  }

  fun setOverlayColor(color: Int) {
    strokePaint.color = color
    // Auto-update fill color if no custom fill color is set
    if (customFillColor == null) {
      updateFillColorFromStroke()
    }
    invalidate()
  }

  fun setOverlayFillColor(color: Int?) {
    customFillColor = color
    if (color != null) {
      fillPaint.color = color
    } else {
      // If null, auto-generate from stroke color
      updateFillColorFromStroke()
    }
    invalidate()
  }

  private fun updateFillColorFromStroke() {
    val strokeColor = strokePaint.color
    fillPaint.color = Color.argb(60, Color.red(strokeColor), Color.green(strokeColor), Color.blue(strokeColor))
  }

  fun getCurrentPoints(): List<PointF> {
    return if (hasFirstPoints) currentPoints.toList() else emptyList()
  }

  fun setOverlayStrokeWidth(width: Float) {
    strokePaint.strokeWidth = width
    invalidate()
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    if (!hasFirstPoints) return

    path.reset()
    path.moveTo(currentPoints[0].x, currentPoints[0].y)
    for (i in 1..3) {
      path.lineTo(currentPoints[i].x, currentPoints[i].y)
    }
    path.close()

    canvas.drawPath(path, fillPaint)
    canvas.drawPath(path, strokePaint)
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    stopAnimation()
  }
}
