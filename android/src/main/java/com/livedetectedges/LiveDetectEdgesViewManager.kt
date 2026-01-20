package com.livedetectedges

import android.graphics.Color
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.ViewManagerDelegate
import com.facebook.react.uimanager.annotations.ReactProp
import com.facebook.react.viewmanagers.LiveDetectEdgesViewManagerInterface
import com.facebook.react.viewmanagers.LiveDetectEdgesViewManagerDelegate
import com.facebook.react.uimanager.PixelUtil

@ReactModule(name = LiveDetectEdgesViewManager.NAME)
class LiveDetectEdgesViewManager : SimpleViewManager<LiveDetectEdgesView>(),
  LiveDetectEdgesViewManagerInterface<LiveDetectEdgesView> {
  private val mDelegate: ViewManagerDelegate<LiveDetectEdgesView>

  init {
    mDelegate = LiveDetectEdgesViewManagerDelegate(this)
  }

  override fun getDelegate(): ViewManagerDelegate<LiveDetectEdgesView>? {
    return mDelegate
  }

  override fun getName(): String {
    return NAME
  }

  public override fun createViewInstance(context: ThemedReactContext): LiveDetectEdgesView {
    return LiveDetectEdgesView(context)
  }

  @ReactProp(name = "overlayColor")
  override fun setOverlayColor(view: LiveDetectEdgesView?, color: Int?) {
    view?.setOverlayColor(color ?: Color.GREEN)
  }

  @ReactProp(name = "overlayFillColor")
  override fun setOverlayFillColor(view: LiveDetectEdgesView?, color: Int?) {
    view?.setOverlayFillColor(color)
  }

  @ReactProp(name = "overlayStrokeWidth")
  override fun setOverlayStrokeWidth(view: LiveDetectEdgesView, value: Float) {
    view.setOverlayStrokeWidth(PixelUtil.toPixelFromDIP(value))
  }

  companion object {
    const val NAME = "LiveDetectEdgesView"
  }
}
