package com.dndsheet.app.ui.character.layout

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize

/**
 * Two-finger pinch-to-zoom + pan over a single subtree, treating every child
 * layer (PDF background, sheet boxes, ink strokes) as one rigid piece. It does
 * **not** transform anything itself — it only reports the new [scale]/[offset]
 * via [onChange]; the caller applies them with a single `graphicsLayer` that
 * wraps all the layers, so they scale together.
 *
 * **Placement**: put this modifier *outside* (to the left of) the
 * `graphicsLayer` that consumes [scale]/[offset]. That keeps the centroid/pan
 * it reads in the untransformed layout space — the same space [offset] lives
 * in — while the children below the `graphicsLayer` receive correctly
 * transformed pointer coordinates (so ink drawn while zoomed lands on the
 * right spot and stays aligned with the boxes).
 *
 * **Coexistence with drawing/editing/scrolling**: detection runs in
 * [PointerEventPass.Initial] and only acts — and consumes — when two or more
 * pointers are down. Single-finger gestures are never touched, so freehand
 * drawing, box drag/resize and the parent vertical scroll all pass straight
 * through.
 *
 * State is read through provider lambdas so the always-`Unit`-keyed
 * `pointerInput` block sees fresh values on every gesture without restarting.
 *
 * @param scaleProvider  current scale factor (1f = unzoomed).
 * @param offsetProvider current translation, in untransformed layout pixels.
 * @param sizeProvider   current (unscaled) size of the zoomed box, for edge
 *                       clamping; pass [IntSize.Zero] before it's measured.
 * @param onChange       receives the updated (scale, offset) each frame.
 */
fun Modifier.pinchToZoom(
    scaleProvider: () -> Float,
    offsetProvider: () -> Offset,
    sizeProvider: () -> IntSize,
    minScale: Float = 1f,
    maxScale: Float = 5f,
    onChange: (scale: Float, offset: Offset) -> Unit
): Modifier = this.pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var curScale = scaleProvider()
        var curOffset = offsetProvider()
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val pressedCount = event.changes.count { it.pressed }
            if (pressedCount >= 2) {
                val zoom = event.calculateZoom()
                val pan = event.calculatePan()
                val centroid = event.calculateCentroid(useCurrent = true)
                if (zoom != 1f || pan != Offset.Zero) {
                    val newScale = (curScale * zoom).coerceIn(minScale, maxScale)
                    var newOffset = if (newScale <= minScale) {
                        // Fully zoomed out → snap back to a clean, origin-aligned view.
                        Offset.Zero
                    } else {
                        // Keep the gesture centroid anchored to the same content
                        // point while zooming, then apply the two-finger pan.
                        centroid + pan - (centroid - curOffset) * (newScale / curScale)
                    }
                    // Clamp so the content edges can't be dragged inside the
                    // viewport (no blank gutters above/left of the sheet).
                    val sz = sizeProvider()
                    if (newScale > minScale && sz.width > 0 && sz.height > 0) {
                        val minX = sz.width * (1f - newScale)
                        val minY = sz.height * (1f - newScale)
                        newOffset = Offset(
                            newOffset.x.coerceIn(minX, 0f),
                            newOffset.y.coerceIn(minY, 0f)
                        )
                    }
                    curScale = newScale
                    curOffset = newOffset
                    onChange(curScale, curOffset)
                }
                // Consume so the layers below never draw/drag during a pinch.
                event.changes.forEach { it.consume() }
            }
        } while (event.changes.any { it.pressed })
    }
}
