package com.phamnhantucode.colorpicker

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Color history manager with SharedPreferences persistence.
 *
 * This class manages persistent color history using Android's SharedPreferences.
 * Users of this library need to provide a Context to enable history persistence.
 *
 * @param context Android context for SharedPreferences access
 * @param config Configuration for history behavior
 */
class ColorHistory(
    private val context: Context,
    private val config: ColorPickerConfig = ColorPickerConfig()
) {
    private val prefs = context.getSharedPreferences(
        config.historyPrefsName,
        Context.MODE_PRIVATE
    )

    /**
     * Retrieves the list of previously used colors from history.
     *
     * @return List of colors from history, ordered from most to least recent
     */
    fun getColors(): List<Color> {
        val colorStrings = prefs.getString("colors", "") ?: ""
        if (colorStrings.isEmpty()) return emptyList()

        return colorStrings.split(",")
            .mapNotNull { hexString ->
                try {
                    Color(android.graphics.Color.parseColor(hexString))
                } catch (e: Exception) {
                    null
                }
            }
    }

    /**
     * Adds a color to the history.
     *
     * If the color already exists in history, it will be moved to the front.
     * The history is limited to [ColorPickerConfig.maxHistoryColors] items.
     *
     * @param color The color to add to history
     */
    fun addColor(color: Color) {
        val currentColors = getColors().toMutableList()

        // Remove if already exists (to avoid duplicates)
        currentColors.removeAll { it == color }

        // Add to front
        currentColors.add(0, color)

        // Keep only max colors
        val colorsToSave = currentColors.take(config.maxHistoryColors)

        val colorStrings = colorsToSave.joinToString(",") { c ->
            String.format("#%08X", c.toArgb())
        }

        prefs.edit().putString("colors", colorStrings).apply()
    }

    /**
     * Clears all colors from the history.
     */
    fun clearHistory() {
        prefs.edit().remove("colors").apply()
    }
}
