package com.phamnhantucode.colorpicker

/**
 * Configuration options for the color picker dialog.
 *
 * @param showAlpha Whether to display the alpha/opacity slider
 * @param showColorHistory Whether to display the recent colors tab
 * @param animateColorChanges Whether to animate color transitions
 * @param maxHistoryColors Maximum number of colors to store in history (default: 8)
 * @param historyPrefsName SharedPreferences name for color history storage
 */
data class ColorPickerConfig(
    val showAlpha: Boolean = true,
    val showColorHistory: Boolean = true,
    val animateColorChanges: Boolean = true,
    val maxHistoryColors: Int = 8,
    val historyPrefsName: String = "color_picker_history"
)
