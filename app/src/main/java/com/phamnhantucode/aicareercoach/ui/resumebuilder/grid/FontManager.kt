package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.res.ResourcesCompat
import com.phamnhantucode.aicareercoach.R

/**
 * Font Manager
 *
 * Centralized font management for both Compose (Canvas Editor) and Android Canvas (PDF Export).
 * Ensures consistent typography between editor and exported PDF.
 */
object FontManager {

    /**
     * Poppins FontFamily for Compose UI (Canvas Editor)
     *
     * This is used by all text elements in the canvas editor.
     * Compose automatically selects the correct font file based on fontWeight and fontStyle.
     */
    val poppinsFontFamily = FontFamily(
        // Thin 100
        Font(R.font.poppins_thin, FontWeight.Thin, FontStyle.Normal),
        Font(R.font.poppins_thin_italic, FontWeight.Thin, FontStyle.Italic),

        // ExtraLight 200
        Font(R.font.poppins_extra_light, FontWeight.ExtraLight, FontStyle.Normal),
        Font(R.font.poppins_extra_light_italic, FontWeight.ExtraLight, FontStyle.Italic),

        // Light 300
        Font(R.font.poppins_light, FontWeight.Light, FontStyle.Normal),
        Font(R.font.poppins_light_italic, FontWeight.Light, FontStyle.Italic),

        // Regular 400
        Font(R.font.poppins_regular, FontWeight.Normal, FontStyle.Normal),
        Font(R.font.poppins_italic, FontWeight.Normal, FontStyle.Italic),

        // Medium 500
        Font(R.font.poppins_medium, FontWeight.Medium, FontStyle.Normal),
        Font(R.font.poppins_medium_italic, FontWeight.Medium, FontStyle.Italic),

        // SemiBold 600
        Font(R.font.poppins_semi_bold, FontWeight.SemiBold, FontStyle.Normal),
        Font(R.font.poppins_semi_bold_italic, FontWeight.SemiBold, FontStyle.Italic),

        // Bold 700
        Font(R.font.poppins_bold, FontWeight.Bold, FontStyle.Normal),
        Font(R.font.poppins_bold_italic, FontWeight.Bold, FontStyle.Italic),

        // ExtraBold 800
        Font(R.font.poppins_extra_bold, FontWeight.ExtraBold, FontStyle.Normal),
        Font(R.font.poppins_extra_bold_italic, FontWeight.ExtraBold, FontStyle.Italic),

        // Black 900
        Font(R.font.poppins_black, FontWeight.Black, FontStyle.Normal),
        Font(R.font.poppins_black_italic, FontWeight.Black, FontStyle.Italic)
    )

    /**
     * Get Poppins Typeface for Android Canvas (PDF Export)
     *
     * Maps FontWeight (100-900) to the correct Poppins .ttf file.
     * This ensures PDF export matches the canvas editor appearance.
     *
     * @param context Android context for resource access
     * @param fontWeight Compose FontWeight (Thin, ExtraLight, Light, Normal, Medium, SemiBold, Bold, ExtraBold, Black)
     * @param isItalic Whether to use italic variant
     * @return Android Typeface for the specified weight and style
     */
    fun getPoppinsTypeface(
        context: Context,
        fontWeight: FontWeight,
        isItalic: Boolean
    ): Typeface {
        val fontResId = when (fontWeight.weight) {
            in 0..150 -> {
                // Thin 100
                if (isItalic) R.font.poppins_thin_italic else R.font.poppins_thin
            }
            in 151..250 -> {
                // ExtraLight 200
                if (isItalic) R.font.poppins_extra_light_italic else R.font.poppins_extra_light
            }
            in 251..350 -> {
                // Light 300
                if (isItalic) R.font.poppins_light_italic else R.font.poppins_light
            }
            in 351..450 -> {
                // Regular 400 (default)
                if (isItalic) R.font.poppins_italic else R.font.poppins_regular
            }
            in 451..550 -> {
                // Medium 500
                if (isItalic) R.font.poppins_medium_italic else R.font.poppins_medium
            }
            in 551..650 -> {
                // SemiBold 600
                if (isItalic) R.font.poppins_semi_bold_italic else R.font.poppins_semi_bold
            }
            in 651..750 -> {
                // Bold 700
                if (isItalic) R.font.poppins_bold_italic else R.font.poppins_bold
            }
            in 751..850 -> {
                // ExtraBold 800
                if (isItalic) R.font.poppins_extra_bold_italic else R.font.poppins_extra_bold
            }
            else -> {
                // Black 900
                if (isItalic) R.font.poppins_black_italic else R.font.poppins_black
            }
        }

        return ResourcesCompat.getFont(context, fontResId) ?: Typeface.DEFAULT
    }

    /**
     * Get Poppins Typeface using TextStyle (convenience method)
     *
     * @param context Android context
     * @param textStyle TextStyle from element
     * @return Android Typeface
     */
    fun getPoppinsTypeface(context: Context, textStyle: TextStyle): Typeface {
        return getPoppinsTypeface(context, textStyle.fontWeight, textStyle.isItalic)
    }
}
