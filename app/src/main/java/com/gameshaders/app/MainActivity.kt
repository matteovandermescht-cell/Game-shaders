package com.gameshaders.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

class MainActivity : Activity() {
    private var selectedPreset = ShaderPreset.NIGHT
    private var intensity = 32
    private lateinit var status: TextView
    private lateinit var intensityLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(12, 14, 22)
        window.navigationBarColor = Color.rgb(12, 14, 22)
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) updateStatus()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(24), dp(22), dp(22))
            setBackgroundColor(Color.rgb(12, 14, 22))
        }
        val scroll = android.widget.ScrollView(this)
        scroll.addView(root)
        root.addView(TextView(this).apply {
            text = "GAME SHADERS"
            textSize = 12f
            letterSpacing = 0.18f
            setTextColor(Color.rgb(111, 170, 255))
        })
        root.addView(TextView(this).apply {
            text = "Tune your view."
            textSize = 30f
            setTextColor(Color.WHITE)
            setPadding(0, dp(8), 0, dp(4))
        })
        root.addView(TextView(this).apply {
            text = "Visual presets for your gaming screen."
            textSize = 14f
            setTextColor(Color.rgb(166, 174, 194))
        })
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundColor(Color.rgb(25, 29, 43))
        }
        root.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(24) })
        card.addView(label("VISUAL PRESET"))
        ShaderPreset.values().forEach { preset ->
            card.addView(Button(this).apply {
                text = preset.title
                isAllCaps = false
                setOnClickListener {
                    selectedPreset = preset
                    ShaderOverlayService.preset = preset
                    ShaderOverlayService.intensity = intensity
                    updatePresetButtons(card)
                    if (ShaderOverlayService.isRunning) startOverlay()
                    updateStatus()
                }
            }, matchWidthWrap())
        }
        intensityLabel = label("INTENSITY · " + intensity + "%")
        intensityLabel.setPadding(0, dp(12), 0, dp(4))
        card.addView(intensityLabel)
        card.addView(SeekBar(this).apply {
            max = 70
            progress = intensity
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    intensity = progress
                    intensityLabel.text = "INTENSITY · " + intensity + "%"
                    ShaderOverlayService.intensity = intensity
                    if (fromUser && ShaderOverlayService.isRunning) startOverlay()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        })
        root.addView(Button(this).apply {
            text = "Enable visual overlay"
            isAllCaps = false
            setOnClickListener { enableOverlay() }
        }, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(18) })
        root.addView(Button(this).apply {
            text = "Turn overlay off"
            isAllCaps = false
            setOnClickListener {
                stopService(Intent(this@MainActivity, ShaderOverlayService::class.java))
                updateStatus()
            }
        }, matchWidthWrap())
        status = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.rgb(166, 174, 194))
            setPadding(0, dp(16), 0, dp(8))
        }
        root.addView(status)
        root.addView(TextView(this).apply {
            text = "This applies a transparent screen overlay; it cannot replace a game's internal shader files. Some games may restrict overlays."
            textSize = 12f
            setTextColor(Color.rgb(130, 139, 160))
            setPadding(0, dp(8), 0, 0)
        })
        setContentView(scroll)
        updatePresetButtons(card)
        updateStatus()
    }

    private fun enableOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + packageName)))
            return
        }
        startOverlay()
    }

    private fun startOverlay() {
        if (!Settings.canDrawOverlays(this)) { updateStatus(); return }
        ShaderOverlayService.preset = selectedPreset
        ShaderOverlayService.intensity = intensity
        startService(Intent(this, ShaderOverlayService::class.java))
        updateStatus()
    }

    private fun updatePresetButtons(container: LinearLayout) {
        for (index in 0 until container.childCount) {
            val child = container.getChildAt(index)
            if (child is Button) {
                val match = ShaderPreset.values().firstOrNull { it.title == child.text.toString() }
                if (match != null) child.alpha = if (match == selectedPreset) 1f else 0.68f
            }
        }
    }

    private fun updateStatus() {
        if (!::status.isInitialized) return
        status.text = when {
            !Settings.canDrawOverlays(this) -> "Status: allow display over other apps to enable the overlay."
            ShaderOverlayService.isRunning -> "Status: overlay active · " + selectedPreset.title
            else -> "Status: overlay off"
        }
    }

    private fun label(value: String) = TextView(this).apply {
        text = value
        textSize = 12f
        setTextColor(Color.rgb(111, 170, 255))
        setPadding(0, dp(4), 0, dp(8))
    }
    private fun matchWidthWrap() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
