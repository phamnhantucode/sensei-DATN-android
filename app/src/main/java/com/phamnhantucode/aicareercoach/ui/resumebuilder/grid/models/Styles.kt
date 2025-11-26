package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models

import androidx.compose.ui.text.font.FontWeight

/**
 * Global styling defaults
 */
data class GlobalStyles(
    val defaultFontSize: Float = 14f,
    val defaultTextColor: Long = 0xFF000000,
    val defaultFontWeight: Int = 400, // FontWeight.Normal
    val accentColor: Long = 0xFF2196F3
)

/**
 * Element styling
 */
data class ElementStyle(
    val backgroundColor: Long? = null, // Nullable for transparent
    val borderColor: Long? = null,
    val borderWidth: Float = 0f,
    val borderRadius: Float = 0f,
    val shadowColor: Long? = null,
    val shadowBlur: Float = 0f,
    val shadowOffsetX: Float = 0f,
    val shadowOffsetY: Float = 0f,
    val opacity: Float = 1f
)

/**
 * Text styling
 */
data class TextStyle(
    val fontSize: Float = 14f,
    val fontWeight: FontWeight = FontWeight.Normal,
    val color: Long = 0xFF000000,
    val lineHeight: Float? = null,
    val letterSpacing: Float = 0f,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderlined: Boolean = false,
    val isAllCaps: Boolean = false
)
