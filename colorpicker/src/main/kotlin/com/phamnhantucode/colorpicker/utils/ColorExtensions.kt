package com.phamnhantucode.colorpicker.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.phamnhantucode.colorpicker.models.HSV

/**
 * Extension function to convert a Compose Color to HSV color space.
 *
 * @return HSV representation of the color
 */
fun Color.toHsv(): HSV {
    val hsvArray = FloatArray(3)
    val colorInt = this.toArgb()
    android.graphics.Color.colorToHSV(colorInt, hsvArray)
    return HSV(hsvArray[0], hsvArray[1], hsvArray[2], this.alpha)
}

/**
 * Extension function to convert HSV color space to a Compose Color.
 *
 * @return Compose Color representation
 */
fun HSV.toColor(): Color {
    return Color.hsv(hue, saturation, value, alpha)
}
