package com.gameshaders.app

import android.graphics.Color

enum class ShaderPreset(val title: String, val color: Int) {
    NEUTRAL("Neutral", Color.rgb(235, 242, 255)),
    NIGHT("Night", Color.rgb(30, 52, 100)),
    WARM("Warm", Color.rgb(255, 170, 82)),
    CONTRAST("Contrast", Color.rgb(255, 255, 255))
}
