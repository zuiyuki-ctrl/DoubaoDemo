package org.example.project.ui.chat

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import org.example.project.model.OperatingHandMinPointCount
import org.example.project.model.OperatingHandTouchPoint

fun Modifier.operatingHandTracking(
    enabled: Boolean,
    onTrackReady: (List<OperatingHandTouchPoint>) -> Unit
): Modifier {
    if (!enabled) return this

    return pointerInput(onTrackReady) {
        awaitPointerEventScope {
            var isTracking = false
            var downTime = 0L
            var points = mutableListOf<OperatingHandTouchPoint>()

            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val pressedChanges = event.changes.filter { it.pressed }

                if (pressedChanges.size > 1) {
                    isTracking = false
                    points = mutableListOf()
                    continue
                }

                val change = event.changes.firstOrNull()

                if (!isTracking && pressedChanges.isNotEmpty()) {
                    isTracking = true
                    downTime = pressedChanges.first().uptimeMillis
                    points = mutableListOf()
                }

                if (isTracking && change != null) {
                    points += OperatingHandTouchPoint(
                        x = change.position.x,
                        y = change.position.y,
                        width = size.width.toFloat(),
                        height = size.height.toFloat(),
                        density = density,
                        downTimeDeltaMillis = (change.uptimeMillis - downTime).toFloat()
                    )
                }

                if (isTracking && pressedChanges.isEmpty()) {
                    if (points.size >= OperatingHandMinPointCount) {
                        onTrackReady(points)
                    }
                    isTracking = false
                    points = mutableListOf()
                }
            }
        }
    }
}
