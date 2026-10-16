package com.gameshaders.app

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager

class ShaderOverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var overlay: View? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showOverlay()
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (overlay == null) showOverlay() else updateOverlay()
        return START_NOT_STICKY
    }

    private fun showOverlay() {
        if (overlay != null) return
        val view = View(this).apply { setBackgroundColor(preset.color) }
        val flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type, flags, PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            alpha = intensity.coerceIn(0, 70) / 100f
        }
        overlay = view
        try { windowManager?.addView(view, params) } catch (e: Exception) { overlay = null; stopSelf() }
    }

    private fun updateOverlay() {
        val view = overlay ?: return
        view.setBackgroundColor(preset.color)
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return
        params.alpha = intensity.coerceIn(0, 70) / 100f
        try { windowManager?.updateViewLayout(view, params) } catch (_: Exception) { }
    }

    override fun onDestroy() {
        overlay?.let { try { windowManager?.removeView(it) } catch (_: Exception) { } }
        overlay = null
        isRunning = false
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        @Volatile var preset: ShaderPreset = ShaderPreset.NIGHT
        @Volatile var intensity: Int = 32
        @Volatile var isRunning: Boolean = false
    }
}
