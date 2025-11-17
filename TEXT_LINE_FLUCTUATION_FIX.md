# Text Line Fluctuation Fix - Root Cause Analysis

## Problem Description
When viewing text elements with many lines in the grid editor at different zoom levels (e.g., 60%, 100%), the number of visible lines fluctuates (e.g., switching between 4 and 5 lines) instead of maintaining a consistent line count.

## Root Cause Investigation

### The Chain of Issues

1. **Container Size Changes with Zoom**
   ```
   DraggableElement.kt:
   cellSizePx = cellSizeDp * density * zoomLevel
   width(pixels) = colSpan * cellSizePx
   width(dp) = width(pixels) / density
             = colSpan * cellSizeDp * zoomLevel
   ```

   Example with `colSpan=4, cellSizeDp=20`:
   - At 60% zoom: Container width = `4 * 20 * 0.6 = 48dp`
   - At 100% zoom: Container width = `4 * 20 * 1.0 = 80dp`

2. **Text Properties Scale with Zoom**
   ```kotlin
   // Original code in toComposeTextStyle()
   fontSize = (fontSize * zoomLevel).sp
   lineHeight = (lineHeight * zoomLevel).sp
   ```
   
   - At 60% zoom: `8.4sp` font in `48dp` container
   - At 100% zoom: `14sp` font in `80dp` container

3. **Why Line Count Fluctuates**
   
   Even though both width and font size scale proportionally (both multiply by zoomLevel), **Compose text layout uses pixel-perfect rendering**:
   
   - Text wrapping happens at the **pixel level**
   - `48dp * density` vs `80dp * density` produces different pixel counts
   - `8.4sp * density` vs `14sp * density` produces different pixel fonts
   - Due to **subpixel rounding**, pixel-perfect glyph widths, and font hinting, the ratio doesn't stay exactly equal
   - Result: Text wraps at slightly different character positions → different line counts

4. **The Failing Approach**
   
   Initially tried:
   ```kotlin
   // Normalize width, but still use scaled text
   val normalizedWidth = maxWidth / zoomLevel
   BasicTextField(textStyle = scaledTextStyle) // ❌ Still scales font!
   ```
   
   This doesn't work because the **fontSize itself is still zoomed**, causing different text measurements.

## The Solution

**Use unscaled text measurements and apply zoom as a pure visual transformation:**

### Implementation

```kotlin
// 1. Calculate unscaled text style (always at zoom = 1.0)
val unscaledTextStyle = element.textStyle.toComposeTextStyle(zoomLevel = 1f)

// 2. Normalize container dimensions to zoom level 1.0
BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val normalizedWidth = maxWidth / zoomLevel
    val normalizedHeight = maxHeight / zoomLevel
    
    // 3. Text lays out at normalized size with unscaled font
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
        // Text with unscaled style
        Text(text = text, style = unscaledTextStyle)
    }
}
```

### Why This Works

1. **Consistent Text Measurement**: Text always measures against the same logical dimensions (as if zoom = 1.0)
2. **Consistent Font Size**: Font is always the base size (e.g., 14sp), ensuring identical glyph measurements
3. **Consistent Line Breaking**: Since width and font don't change, line breaks happen at identical positions
4. **Visual Scaling Only**: `graphicsLayer` scales the already-laid-out content without affecting text measurement
5. **Proper Transform Origin**: `TransformOrigin(0f, 0f)` ensures scaling from top-left, keeping text aligned

### Key Changes Made

**File: `TextElementRenderer.kt`**

1. Calculate unscaled text style once at the top:
   ```kotlin
   val unscaledTextStyle = element.textStyle.toComposeTextStyle(zoomLevel = 1f)
   ```

2. Wrap content in normalized Box with graphicsLayer:
   ```kotlin
   BoxWithConstraints {
       val normalizedWidth = maxWidth / zoomLevel
       val normalizedHeight = maxHeight / zoomLevel
       Box(
           modifier = Modifier
               .width(normalizedWidth)
               .height(normalizedHeight)
               .graphicsLayer(scaleX = zoomLevel, scaleY = zoomLevel, ...)
       ) { /* content */ }
   }
   ```

3. Apply unscaled text style to both StaticText and EditableText

4. Removed zoom-dependent padding (changed from `8 * zoomLevel` to fixed `8.dp`)

## Testing

To verify the fix:

1. Create a text element with 5+ lines of content
2. Set zoom to 60% and note the line count
3. Change zoom to 80%, 100%, 120%
4. **Expected**: Line count remains constant
5. **Before fix**: Line count fluctuates between 4 and 5

## Technical Details

### Why Not Scale Padding?
Fixed padding ensures consistent spacing regardless of zoom:
- Zoom-dependent padding changes available width for text
- Changes in available width → changes in line wrapping
- Fixed padding = stable text layout area

### Transform Origin Importance
```kotlin
transformOrigin = TransformOrigin(0f, 0f) // Top-left corner
```
Ensures:
- Scaling happens from top-left
- Text stays aligned with element position
- No unexpected shifts when zooming

### Performance Considerations
- `graphicsLayer` is GPU-accelerated and efficient
- Text measurement happens once at normalized size
- No performance penalty compared to previous approach

## Summary

The fix ensures text elements maintain **consistent line counts across all zoom levels** by:
1. Measuring text at a normalized (unzoomed) size
2. Using unscaled font metrics
3. Applying zoom as a pure visual transformation with `graphicsLayer`

This approach treats zoom as a **rendering concern**, not a layout concern, which is the correct mental model for maintaining consistent visual structure.
