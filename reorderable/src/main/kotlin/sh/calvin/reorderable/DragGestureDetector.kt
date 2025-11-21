package sh.calvin.reorderable

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUp

fun interface DragGestureDetector {
    suspend fun PointerInputScope.detect(
        onDragStart: (Offset) -> Unit,
        onDragEnd: () -> Unit,
        onDragCancel: () -> Unit,
        onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit
    )

    object Press : DragGestureDetector {
        override suspend fun PointerInputScope.detect(
            onDragStart: (Offset) -> Unit,
            onDragEnd: () -> Unit,
            onDragCancel: () -> Unit,
            onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit
        ) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                var drag: PointerInputChange?
                var overSlop = Offset.Zero

                while (true) {
                    val event = awaitPointerEvent()
                    drag = event.changes.firstOrNull { it.id == down.id }

                    if (drag == null || drag.changedToUp()) {
                        break
                    }

                    val dragAmount = drag.position - drag.previousPosition
                    overSlop += dragAmount

                    drag.consume()

                    if (overSlop.getDistance() > viewConfiguration.touchSlop) {
                        onDragStart(down.position)

                        while (true) {
                            val dragEvent = awaitPointerEvent()
                            val dragChange = dragEvent.changes.firstOrNull { it.id == down.id }

                            if (dragChange == null || dragChange.changedToUp()) {
                                onDragEnd()
                                return@awaitEachGesture
                            }

                            val changeAmount = dragChange.position - dragChange.previousPosition
                            if (changeAmount != Offset.Zero) {
                                onDrag(dragChange, changeAmount)
                                if (!dragChange.isConsumed) dragChange.consume()
                            }
                        }
                    }
                }
            }
        }
    }

    object LongPress : DragGestureDetector {
        override suspend fun PointerInputScope.detect(
            onDragStart: (Offset) -> Unit,
            onDragEnd: () -> Unit,
            onDragCancel: () -> Unit,
            onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit
        ) {
            detectDragGesturesAfterLongPress(onDragStart, onDragEnd, onDragCancel, onDrag)
        }
    }
}
