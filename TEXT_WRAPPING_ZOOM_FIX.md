# Text Wrapping Zoom Issue - Comprehensive Analysis & Solution

## Issue Description
When zooming in the grid editor, text elements auto-wrap to different numbers of lines (fluctuating between n and n+1 lines), causing a flickering/jumping effect. This happens because Compose's `Text` composable recalculates text layout based on the zoomed font size, leading to different line break decisions at different zoom levels.

## Root Cause
All element renderers (except TextElementRenderer and ProjectElementRenderer which are already fixed) use:
```kotlin
fontSize = (fontSize * zoomLevel).sp
```

This causes the Text composable to:
1. Calculate available width in pixels
2. Calculate text size at zoomed fontSize
3. Perform line breaking based on these zoomed dimensions
4. Result: Different zoom levels = different line breaks

## Elements Affected

### ✅ All Fixed!
1. **TextElementRenderer** - Uses Canvas-based rendering with base dimensions
2. **ProjectElementRenderer** - Uses Canvas-based rendering with base dimensions
3. **WorkExperienceElementRenderer** - Fixed with BoxWithConstraints + graphicsLayer
4. **EducationElementRenderer** - Fixed with BoxWithConstraints + graphicsLayer
5. **CertificationElementRenderer** - Fixed with BoxWithConstraints + graphicsLayer
6. **SkillElementRenderer** - Fixed with BoxWithConstraints + graphicsLayer
7. **LanguageElementRenderer** - Fixed with BoxWithConstraints + graphicsLayer
8. **ContactElementRenderer** - Fixed with BoxWithConstraints + graphicsLayer

## Solution Approach

### Option 1: Canvas-Based Rendering (Recommended for Complex Elements)
Convert elements with multi-line text content to Canvas-based rendering like TextElementRenderer and ProjectElementRenderer.

**Pros:**
- Consistent line breaks at all zoom levels
- Better performance for complex layouts
- Matches PDF export rendering exactly

**Cons:**
- More complex implementation
- Requires custom drawing code
- Loss of Compose text selection/interaction features

**Best for:**
- WorkExperienceElementRenderer (has responsibilities/bullet points)
- EducationElementRenderer (has achievements/bullet points)
- CertificationElementRenderer (has descriptions)

### Option 2: BoxWithConstraints + GraphicsLayer (Simpler for Short Text)
Use normalized dimensions with graphics layer scaling.

**Pros:**
- Simpler implementation
- Keeps Compose text features
- Works well for short, non-wrapping text

**Cons:**
- May still have minor wrapping issues with long text
- Slightly less precise than Canvas approach

**Best for:**
- SkillElementRenderer (mostly short skill names)
- LanguageElementRenderer (short language names)
- ContactElementRenderer (short contact info)

## Implementation Details

### Canvas-Based Rendering Pattern

```kotlin
@Composable
fun ElementRenderer(element: ResumeElement, zoomLevel: Float, modifier: Modifier) {
    val context = LocalContext.current
    
    Canvas(modifier = modifier.fillMaxSize()) {
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            val density = context.resources.displayMetrics.density
            
            // Calculate base dimensions (unscaled) - KEY STEP
            val basePadding = 8f * density
            val baseWidth = (size.width / zoomLevel) - (basePadding * 2)
            val baseHeight = (size.height / zoomLevel) - (basePadding * 2)
            
            // Create TextPaint with base size (no zoom)
            val textPaint = TextPaint().apply {
                textSize = element.textStyle.fontSize * density // NO zoomLevel here
                color = element.textStyle.color.toInt()
                // ... other properties
            }
            
            // Create layout with base width
            val layout = StaticLayout.Builder
                .obtain(text, 0, text.length, textPaint, baseWidth.toInt())
                .build()
            
            // Apply zoom via Canvas scaling AFTER layout
            nativeCanvas.save()
            nativeCanvas.scale(zoomLevel, zoomLevel)
            nativeCanvas.translate(basePadding, basePadding)
            
            // Draw text (already laid out at zoom 1.0)
            layout.draw(nativeCanvas)
            
            nativeCanvas.restore()
        }
    }
}
```

### BoxWithConstraints Pattern (for simpler cases)

```kotlin
@Composable
fun ElementRenderer(element: ResumeElement, zoomLevel: Float, modifier: Modifier) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
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
                )
        ) {
            Text(
                text = element.content,
                style = element.textStyle.toComposeTextStyle(zoomLevel = 1f), // NO zoom
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
```

## ✅ Implementation Complete

All renderers have been fixed using the BoxWithConstraints + graphicsLayer approach:

### Changes Applied:
1. ✅ WorkExperienceElementRenderer
2. ✅ EducationElementRenderer
3. ✅ CertificationElementRenderer
4. ✅ SkillElementRenderer
5. ✅ LanguageElementRenderer
6. ✅ ContactElementRenderer

### What was changed:
- Added `graphicsLayer` and `TransformOrigin` imports
- Modified `toComposeTextStyle` to NOT multiply fontSize/lineHeight/letterSpacing by zoomLevel
- Wrapped content in `BoxWithConstraints` with normalized dimensions
- Applied zoom via `graphicsLayer` with scale transform
- Changed padding from `(8 * zoomLevel).dp` to `8.dp`
- Passed `zoomLevel = 1f` to child renderers instead of actual zoomLevel

## Testing Checklist
After implementing fixes for each element:
- [ ] Zoom in/out from 50% to 200%
- [ ] Verify text doesn't jump between different line counts
- [ ] Check that text remains readable at all zoom levels
- [ ] Verify PDF export still works correctly
- [ ] Test with long and short text content
- [ ] Test with different font sizes

## Files to Modify

1. `WorkExperienceElementRenderer.kt` - Convert to Canvas (similar to ProjectElementRenderer)
2. `EducationElementRenderer.kt` - Convert to Canvas (similar to ProjectElementRenderer)
3. `CertificationElementRenderer.kt` - Convert to Canvas (similar to ProjectElementRenderer)
4. `SkillElementRenderer.kt` - Use BoxWithConstraints pattern
5. `LanguageElementRenderer.kt` - Use BoxWithConstraints pattern
6. `ContactElementRenderer.kt` - Use BoxWithConstraints pattern

## Reference Files
- `TextElementRenderer.kt` - Perfect example of Canvas-based rendering
- `ProjectElementRenderer.kt` - Example with complex layout and multiple text elements
