package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.elements

import android.graphics.Paint
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.ResumeElement
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.TextAlignment
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.VerticalTextAlignment

/**
 * Renders a text element on the resume
 */
@Composable
fun TextElementRenderer(
    element: ResumeElement.TextElement,
    isEditing: Boolean = false,
    onContentChange: (String) -> Unit = {},
    zoomLevel: Float = 1f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    if (isEditing) {
        // Use old Compose rendering for editing mode
        EditableTextRenderer(element, onContentChange, zoomLevel, modifier)
    } else {
        // Use Canvas-based rendering for static text (like PDF exporter)
        Canvas(modifier = modifier.fillMaxSize()) {
            drawIntoCanvas { canvas ->
                val nativeCanvas = canvas.nativeCanvas
                val bounds = RectF(0f, 0f, size.width, size.height)
                
                // Draw background and borders
                drawElementStyle(nativeCanvas, element.style, bounds, zoomLevel)
                
                // Apply 8dp content padding (matching PDF behavior)
                val contentPadding = 8f * zoomLevel
                val contentBounds = RectF(
                    bounds.left + contentPadding,
                    bounds.top + contentPadding,
                    bounds.right - contentPadding,
                    bounds.bottom - contentPadding
                )
                
                // Create text paint
                val textPaint = createTextPaint(element, zoomLevel, context)
                
                // Create layout for text with padded width
                val layout = createTextLayout(
                    element.content,
                    textPaint,
                    contentBounds.width().toInt().coerceAtLeast(1),
                    element.alignment,
                    element.maxLines,
                    element.textStyle
                )
                
                // Calculate vertical alignment within content bounds
                val topPadding = layout.getLineTop(0).toFloat()
                val textHeight = layout.height.toFloat() - topPadding
                val availableHeight = contentBounds.height()
                val yOffset = when (element.verticalAlignment ?: VerticalTextAlignment.CENTER) {
                    VerticalTextAlignment.TOP -> -topPadding
                    VerticalTextAlignment.CENTER -> (availableHeight - textHeight) / 2f - topPadding
                    VerticalTextAlignment.BOTTOM -> availableHeight - textHeight - topPadding
                }
                
                // Draw text within content bounds
                nativeCanvas.save()
                nativeCanvas.translate(contentBounds.left, contentBounds.top + yOffset)
                nativeCanvas.clipRect(0f, 0f, contentBounds.width(), contentBounds.height())
                layout.draw(nativeCanvas)
                nativeCanvas.restore()
            }
        }
    }
}

/**
 * Draw element background, border, and shadow
 */
private fun drawElementStyle(
    canvas: android.graphics.Canvas,
    style: com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.ElementStyle,
    bounds: RectF,
    zoomLevel: Float
) {
    val paint = Paint().apply {
        isAntiAlias = true
    }
    
    // Draw shadow
    if (style.shadowColor != null && style.shadowBlur > 0f) {
        val shadowPaint = Paint(paint).apply {
            color = style.shadowColor.toInt()
            alpha = (style.opacity * 255).toInt()
            setShadowLayer(
                style.shadowBlur * zoomLevel,
                style.shadowOffsetX * zoomLevel,
                style.shadowOffsetY * zoomLevel,
                style.shadowColor.toInt()
            )
        }
        
        val shadowBounds = RectF(bounds)
        shadowBounds.offset(style.shadowOffsetX * zoomLevel, style.shadowOffsetY * zoomLevel)
        
        if (style.borderRadius > 0f) {
            val radius = style.borderRadius * zoomLevel
            canvas.drawRoundRect(shadowBounds, radius, radius, shadowPaint)
        } else {
            canvas.drawRect(shadowBounds, shadowPaint)
        }
    }
    
    // Draw background
    if (style.backgroundColor != null && Color(style.backgroundColor).alpha > 0f) {
        paint.color = style.backgroundColor.toInt()
        paint.alpha = (style.opacity * 255).toInt()
        paint.style = Paint.Style.FILL
        
        if (style.borderRadius > 0f) {
            val radius = style.borderRadius * zoomLevel
            canvas.drawRoundRect(bounds, radius, radius, paint)
        } else {
            canvas.drawRect(bounds, paint)
        }
    }
    
    // Draw border
    if (style.borderColor != null && style.borderWidth > 0f) {
        paint.color = style.borderColor.toInt()
        paint.alpha = (style.opacity * 255).toInt()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = style.borderWidth * zoomLevel
        
        if (style.borderRadius > 0f) {
            val radius = style.borderRadius * zoomLevel
            canvas.drawRoundRect(bounds, radius, radius, paint)
        } else {
            canvas.drawRect(bounds, paint)
        }
    }
}

/**
 * Create text paint with styling
 */
private fun createTextPaint(
    element: ResumeElement.TextElement,
    zoomLevel: Float,
    context: android.content.Context
): TextPaint {
    val density = context.resources.displayMetrics.density
    return TextPaint().apply {
        isAntiAlias = true
        // Convert fontSize (stored as SP value) to pixels, then apply zoom
        // This matches how Compose Text handles font sizes
        textSize = element.textStyle.fontSize * density * zoomLevel
        color = element.textStyle.color.toInt()
        
        // Use Poppins font with proper weight mapping
        typeface = com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.FontManager.getPoppinsTypeface(
            context,
            element.textStyle.fontWeight,
            element.textStyle.isItalic
        )
        
        // Underline
        isUnderlineText = element.textStyle.isUnderlined
        
        // Letter spacing
        letterSpacing = element.textStyle.letterSpacing
    }
}

/**
 * Create text layout with proper alignment and wrapping
 */
private fun createTextLayout(
    text: String,
    paint: TextPaint,
    width: Int,
    alignment: TextAlignment,
    maxLines: Int?,
    textStyle: com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.TextStyle
): StaticLayout {
    val layoutAlignment = when (alignment) {
        TextAlignment.LEFT -> Layout.Alignment.ALIGN_NORMAL
        TextAlignment.CENTER -> Layout.Alignment.ALIGN_CENTER
        TextAlignment.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
        TextAlignment.JUSTIFY -> Layout.Alignment.ALIGN_NORMAL
    }
    
    val lineSpacingMultiplier = textStyle.lineHeight?.let {
        it / textStyle.fontSize
    } ?: 1.0f
    
    val builder = StaticLayout.Builder
        .obtain(text, 0, text.length, paint, width.coerceAtLeast(1))
        .setAlignment(layoutAlignment)
        .setLineSpacing(0f, lineSpacingMultiplier)
        .setIncludePad(false)
        .setMaxLines(maxLines ?: Int.MAX_VALUE)
    
    if (alignment == TextAlignment.JUSTIFY && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        builder.setJustificationMode(android.text.Layout.JUSTIFICATION_MODE_INTER_WORD)
    } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        builder.setJustificationMode(android.text.Layout.JUSTIFICATION_MODE_NONE)
    }
    
    return builder.build()
}

/**
 * Editable text renderer using Compose (only for editing mode)
 */
@Composable
private fun EditableTextRenderer(
    element: ResumeElement.TextElement,
    onContentChange: (String) -> Unit,
    zoomLevel: Float,
    modifier: Modifier
) {
    val backgroundColor = element.style.backgroundColor?.let { Color(it) }
    val borderColor = element.style.borderColor?.let { Color(it) }
    val unscaledTextStyle = element.textStyle.toComposeTextStyle(zoomLevel = 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (backgroundColor != null) {
                    Modifier.background(
                        color = backgroundColor,
                        shape = RoundedCornerShape(element.style.borderRadius.dp)
                    )
                } else {
                    Modifier
                }
            )
            .then(
                if (borderColor != null && element.style.borderWidth > 0) {
                    Modifier.border(
                        width = element.style.borderWidth.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(element.style.borderRadius.dp)
                    )
                } else {
                    Modifier
                }
            )
            .then(
                if (element.style.shadowBlur > 0) {
                    Modifier.shadow(
                        elevation = element.style.shadowBlur.dp,
                        shape = RoundedCornerShape(element.style.borderRadius.dp)
                    )
                } else {
                    Modifier
                }
            )
            .padding(8.dp),
        contentAlignment = (element.verticalAlignment ?: VerticalTextAlignment.CENTER).toAlignment()
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val normalizedWidth = maxWidth / zoomLevel
            val normalizedHeight = maxHeight / zoomLevel
            
            Box(
                modifier = Modifier
                    .width(normalizedWidth)
                    .height(normalizedHeight)
                    .graphicsLayer(
                        scaleX = zoomLevel,
                        scaleY = zoomLevel,
                        transformOrigin = TransformOrigin(0f, 0f)
                    ),
                contentAlignment = Alignment.TopStart
            ) {
                EditableText(
                    text = element.content,
                    textStyle = unscaledTextStyle,
                    alignment = element.alignment,
                    onContentChange = onContentChange
                )
            }
        }
    }
}

/**
 * Editable text field
 */
@Composable
private fun EditableText(
    text: String,
    textStyle: TextStyle,
    alignment: TextAlignment,
    onContentChange: (String) -> Unit
) {
    var textValue by remember { mutableStateOf(text) }

    LaunchedEffect(text) {
        if (text != textValue) {
            textValue = text
        }
    }

    BasicTextField(
        value = textValue,
        onValueChange = {
            textValue = it
            onContentChange(it)
        },
        textStyle = textStyle.copy(
            textAlign = alignment.toTextAlign()
        ),
        cursorBrush = SolidColor(Color.Blue),
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(4.dp)
    )
}

/**
 * Converts custom TextStyle to Compose TextStyle
 */
private fun com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.TextStyle.toComposeTextStyle(zoomLevel: Float = 1f): TextStyle {
    val scaledFontSize = (fontSize * zoomLevel).sp
    return TextStyle(
        fontSize = scaledFontSize,
        fontWeight = fontWeight,
        fontFamily = com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.FontManager.poppinsFontFamily,
        color = Color(color),
        lineHeight = lineHeight?.let { 
            val scaledLineHeight = (it * zoomLevel).sp
            scaledLineHeight
        } ?: (scaledFontSize.value * 1.5f).sp,
        letterSpacing = (letterSpacing * zoomLevel).sp,
        fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
        textDecoration = if (isUnderlined) TextDecoration.Underline else TextDecoration.None
    )
}

/**
 * Converts custom TextAlignment to Compose TextAlign
 */
private fun TextAlignment.toTextAlign(): TextAlign {
    return when (this) {
        TextAlignment.LEFT -> TextAlign.Left
        TextAlignment.CENTER -> TextAlign.Center
        TextAlignment.RIGHT -> TextAlign.Right
        TextAlignment.JUSTIFY -> TextAlign.Justify
    }
}

/**
 * Converts VerticalTextAlignment to Compose Alignment
 */
private fun VerticalTextAlignment.toAlignment(): Alignment {
    return when (this) {
        VerticalTextAlignment.TOP -> Alignment.TopStart
        VerticalTextAlignment.CENTER -> Alignment.CenterStart
        VerticalTextAlignment.BOTTOM -> Alignment.BottomStart
    }
}
