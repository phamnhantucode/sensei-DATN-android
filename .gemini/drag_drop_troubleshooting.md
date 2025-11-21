# 🔧 Drag-and-Drop Troubleshooting & Usage Guide

## ✅ How Drag-and-Drop Now Works

### **Architecture**

1. **Drag Handle Icon (⋮⋮)** - Has `detectDragGesturesAfterLongPress`
   - Long-press to start drag
   - Calls `onDragStart()` → Sets `draggedItemId`
   - Consumes drag events
   - Calls `onDrop()` on release

2. **Item Container (Box)** - Has `awaitPointerEventScope`
   - Detects when pointer hovers over it during drag
   - Calculates relative Y position
   - Calls `onDragEnter(relativeY)` → Sets `dropTargetId` and `dropMode`

3. **State Management**
   - `draggedItemId` - Which item is being dragged
   - `dropTargetId` - Which item is the drop target
   - `dropMode` - REORDER or INTO_CONTAINER

### **Step-by-Step User Flow**

1. **Long-press** the drag handle icon (⋮⋮)
   - Icon color changes to primary
   - Item background changes to primary (30% opacity)
   - Item elevates 8dp with shadow

2. **While holding**, drag up or down
   - As you hover over other items, they detect your pointer
   - Drop target gets visual indicator:
     - **Top 30%** = Blue line (REORDER mode)
     - **Bottom 70% of Frame** = Colored border (INTO_CONTAINER mode)

3. **Release** to drop
   - Calls appropriate action:
     - REORDER: `onMoveLayer(fromIndex, toIndex)`
     - INTO_CONTAINER: `onMoveToContainer(elementId, containerId)`
   - Resets all drag state

## 🎯 Testing Checklist

**Basic Drag:**
- [ ] Long-press drag handle for 300-500ms
- [ ] Item should elevate and change color
- [ ] Drag handle icon should turn primary color

**Hover Detection:**
- [ ] While dragging, move over another item
- [ ] Top 30%: Should show blue line
- [ ] Bottom 70% of Frame: Should show colored border

**Drop:**
- [ ] Release to drop
- [ ] Item order should change (reorder)
- [ ] OR item should nest inside frame (container)

**Visual Feedback:**
- [ ] Dragged item has 8dp elevation
- [ ] Dragged item has primary background (30% opacity)
- [ ] Drop target shows indicator (line or border)

## 🐛 Common Issues & Solutions

### **Issue: Long-press doesn't start drag**

**Possible causes:**
1. Not pressing long enough (need ~500ms)
2. Clicking instead of pressing
3. gesture being intercepted

**Solution:**
- Hold finger/mouse down for a full second on the drag handle
- Don't move while pressing

### **Issue: No visual feedback when dragging**

**Possible causes:**
1. State not updating (`isDragging` not true)
2. Recomposition not happening

**Check:**
```kotlin
// In HierarchicalLayerItem
val isDragging = draggedItemId == item.element.id
// Should be true when dragging
```

### **Issue: Drop target not detected**

**Possible causes:**
1. `awaitPointerEventScope` not receiving events
2. Other pointer inputs consuming events

**Solution:**
- Ensure `change.pressed && change.previousPressed` is true
- Verify `onDragEnter` is being called with Log statements

### **Issue: Drop doesn't work (no reorder)**

**Possible causes:**
1. `onDrop` not being called
2. `draggedItemId` or `dropTargetId` is null
3. ViewModel function not updating state

**Check:**
```kotlin
// Should call onMoveLayer or onMoveToContainer
// Verify indices are correct
// Check ViewModel updates _elements StateFlow
```

## 📊 Debug Logging

Add these to track drag state:

```kotlin
// In onDragStart
Log.d("LayersPanel", "Drag started: ${item.element.id}")

// In onDragEnter
Log.d("LayersPanel", "Hover over: ${item.element.id}, Y: $offsetY, Mode: $dropMode")

// In onDrop
Log.d("LayersPanel", "Drop: dragged=$draggedItemId, target=$dropTargetId, mode=$dropMode")
```

## 🎨 Visual Indicators

| State | Visual Effect |
|-------|--------------|
| **Dragging** | • 8dp elevation<br>• Primary background (30%)<br>• Primary drag handle | | **Drop Target (Reorder)** | • 4dp blue divider at top |
| **Drop Target (Nest)** | • 3dp colored border around |
| **Selected** | • Primary container background (30%) |
| **Normal** | • Transparent background |

## 🔍 Key Code Sections

### Drag Initialization
```kotlin
.pointerInput(item.element.id) {
    detectDragGesturesAfterLongPress(
        onDragStart = { onDragStart() },
        onDrag = { change, _ -> change.consume() },
        onDragEnd = { onDrop() },
        onDragCancel = { onDragExit() }
    )
}
```

### Hover Detection
```kotlin
.pointerInput(item.element.id) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            event.changes.forEach { change ->
                if (change.pressed && change.previousPressed) {
                    val relativeY = change.position.y / itemHeight
                    onDragEnter(relativeY.coerceIn(0f, 1f))
                }
            }
        }
    }
}
```

### Drop Mode Calculation
```kotlin
dropMode = if (item.element is ResumeElement.ContainerElement && offsetY > 0.3f) {
    DropMode.INTO_CONTAINER
} else {
    DropMode.REORDER
}
```

## ✅ Expected Behavior

**Reorder:**
1. Long-press drag handle on "Text Element 1"
2. Drag over "Text Element 2"
3. See blue line at top of "Text Element 2"
4. Release
5. "Text Element 1" moves above "Text Element 2"
6. Z-index updates

**Nest into Frame:**
1. Long-press drag handle on "Text Element 1"
2. Drag over "Frame Element" (bottom area)
3. See colored border around "Frame Element"
4. Release
5. "Text Element 1" moves inside frame
6. Shows indented under frame when expanded

## 🎯 Next Steps

If dragging still doesn't work:
1. Add debug logs to see which callbacks are firing
2. Check if long-press threshold is being met
3. Verify pointer events aren't being consumed elsewhere
4. Test on different devices (touch vs mouse)

The implementation now has proper hover detection on each item's container, so dragging should work smoothly!
