# Wrap Content Height Caching Implementation

## Problem
When an element has `heightMode == WRAP_CONTENT`, the grid editor would:
1. Keep the previous `rowSpan` height value
2. Only change the `heightMode` flag to `WRAP_CONTENT`  
3. Calculate the actual content height dynamically at render time
4. **Never store this calculated height back** into the element's properties

This meant the height was recalculated every single frame, which is inefficient.

## Solution
Implemented a caching mechanism to store and reuse the calculated wrapped height:

### 1. Added `cachedHeightDp` to GridPosition
**File**: `GridPosition.kt`

Added a new optional field to store the calculated height in density-independent pixels:
```kotlin
data class GridPosition(
    // ... existing fields ...
    val cachedHeightDp: Float? = null  // Cached height when heightMode is WRAP_CONTENT
)
```

### 2. Modified DraggableElement to Use and Store Cached Height
**File**: `DraggableElement.kt` (lines 99-126)

The height calculation logic now:
1. **Checks for cached height first**: If `cachedHeightDp` is available, use it directly
2. **Calculates if not cached**: If null, calculate the actual content height
3. **Stores the result**: Converts the calculated pixel height back to dp and stores it via `onResize`

```kotlin
val height = if (element.position.heightMode == SizeMode.WRAP_CONTENT) {
    if (element.position.cachedHeightDp != null) {
        // Use cached height (fast path)
        element.position.cachedHeightDp * density * zoomLevel
    } else {
        // Calculate and store (slow path, only happens once)
        val actualContentHeight = calculateContentHeight(element, width, density, zoomLevel, context)
        val finalHeight = actualContentHeight ?: fallbackHeight
        
        // Store calculated height back into element
        val heightInDp = finalHeight / (density * zoomLevel)
        val newPosition = element.position.copy(cachedHeightDp = heightInDp)
        
        if (actualContentHeight != null) {
            onResize(element, newPosition)  // Persist the cached height
        }
        
        finalHeight
    }
} else {
    element.position.rowSpan * cellSizePx
}
```

### 3. Clear Cache When Mode Changes
**File**: `PropertyPanel.kt` (line 427)

When the user toggles `heightMode`, we clear the cached height so it recalculates:
```kotlin
val newPosition = element.position.copy(
    heightMode = newMode, 
    cachedHeightDp = null  // Clear cache to force recalculation
)
```

## Benefits
1. **Performance**: Height is only calculated once when switching to WRAP_CONTENT, then reused
2. **Consistency**: The calculated height is persisted and survives element updates
3. **No unnecessary recalculation**: Avoids expensive text layout calculations every frame
4. **Automatic invalidation**: Cache is cleared when mode changes, ensuring correctness

## Flow
1. User sets `heightMode = WRAP_CONTENT` → `cachedHeightDp = null`
2. Next render, `DraggableElement` sees `cachedHeightDp == null`
3. Calculates actual content height using `calculateContentHeight()`
4. Stores result in `cachedHeightDp` via `onResize` callback
5. Future renders use the cached value (fast!)
6. If user changes content or mode, cache is cleared and process repeats

## No PDF Changes Required
The PDF export already works correctly because it calculates heights independently during PDF generation. This change only affects the grid editor's rendering performance.
