package com.phamnhantucode.colorpicker.models

/**
 * HSV (Hue, Saturation, Value) color representation with alpha channel.
 *
 * @param hue The hue component (0-360 degrees)
 * @param saturation The saturation component (0-1)
 * @param value The value/brightness component (0-1)
 * @param alpha The alpha/opacity component (0-1)
 */
data class HSV(
    val hue: Float,        // 0-360
    val saturation: Float, // 0-1
    val value: Float,      // 0-1
    val alpha: Float = 1f  // 0-1
)
