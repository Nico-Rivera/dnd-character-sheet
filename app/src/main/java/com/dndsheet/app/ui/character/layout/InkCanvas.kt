package com.dndsheet.app.ui.character.layout

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.dndsheet.domain.model.Stroke
import com.dndsheet.domain.model.StrokePoint
import java.util.UUID

/**
 * Ink color palette — ARGB ints. Stored in [Stroke.color] as [Long] via
 * [Int.toLong]; restored via `Color(storedLong.toInt())`.
 */
val INK_PALETTE: List<Int> = listOf(
    0xFF1A1A1A.toInt(),  // near-black (default)
    0xFFB71C1C.toInt(),  // dark red
    0xFF1565C0.toInt(),  // dark blue
    0xFF2E7D32.toInt(),  // dark green
    0xFF6A1B9A.toInt(),  // purple
    0xFF4E342E.toInt(),  // dark brown
    0xFFE65100.toInt(),  // burnt orange
    0xFFFAFAFA.toInt(),  // white
)

/** Default pen color (near-black) as a Long for [Stroke.color]. */
val DEFAULT_INK_COLOR: Long = INK_PALETTE[0].toLong()

/** Default pen width in dp. */
const val DEFAULT_INK_WIDTH: Float = 4f

/**
 * Transparent overlay that captures touch input and renders ink strokes.
 * Sits above [SheetCanvas] inside the PDF+canvas [Box] via
 * [Modifier.matchParentSize] + later-sibling draw order.
 *
 * **Touch capture**: touch is only active when [inkMode] is true. Each
 * [InkTool] branch handles its own gestures; empty branches in this
 * commit are filled in by commits 11 (ERASER) and 12 (SELECTION).
 * When [inkMode] is false the coroutine exits before entering any
 * pointer-event scope, so events pass through to the sheet boxes.
 *
 * **Coordinate space**: stroke points are in canvas-local pixels, matching
 * the pointer positions reported by [pointerInput]. [Stroke.width] is
 * stored in dp and converted to pixels at draw time.
 *
 * **Undo / redo**: managed by the caller; this composable only reports
 * completed strokes via [onStrokeComplete].
 */
@Composable
fun InkCanvas(
    strokes: List<Stroke>,
    inkMode: Boolean,
    activeTool: InkTool,
    penColor: Long,
    penWidthDp: Float,
    onStrokeComplete: (Stroke) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    // Always-current refs — safe to read from a long-lived coroutine without
    // restarting it every time the caller lambda or color/width changes.
    val latestPenColor          by rememberUpdatedState(penColor)
    val latestPenWidthDp        by rememberUpdatedState(penWidthDp)
    val latestOnStrokeComplete  by rememberUpdatedState(onStrokeComplete)

    // In-progress stroke (cleared on each new DOWN event).
    val currentPoints = remember { mutableStateListOf<StrokePoint>() }
    var isDrawing by remember { mutableStateOf(false) }

    Canvas(
        modifier = modifier
            // Only add pointerInput when ink mode is actually on. When the
            // modifier is absent the Canvas is a pure drawing surface and
            // touches fall through to the sheet boxes below — this is what
            // lets edit-mode box dragging work normally while ink mode is off.
            // (A pointerInput block that returns early still registers as a
            // pointer-input participant in Z-order hit testing, so we must
            // remove the modifier entirely rather than short-circuit inside it.)
            .then(
                if (inkMode) Modifier.pointerInput(activeTool) {
                    // Clear any stale state from a previous tool.
                    isDrawing = false
                    currentPoints.clear()

                    when (activeTool) {
                        InkTool.PEN -> {
                        awaitPointerEventScope {
                            while (true) {
                                val event  = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: continue
                                when (event.type) {
                                    PointerEventType.Press -> {
                                        currentPoints.clear()
                                        currentPoints.add(
                                            StrokePoint(change.position.x, change.position.y)
                                        )
                                        isDrawing = true
                                        change.consume()
                                    }
                                    PointerEventType.Move -> {
                                        if (isDrawing) {
                                            currentPoints.add(
                                                StrokePoint(change.position.x, change.position.y)
                                            )
                                            change.consume()
                                        }
                                    }
                                    PointerEventType.Release -> {
                                        if (isDrawing && currentPoints.isNotEmpty()) {
                                            latestOnStrokeComplete(
                                                Stroke(
                                                    id     = UUID.randomUUID().toString(),
                                                    points = currentPoints.toList(),
                                                    color  = latestPenColor,
                                                    width  = latestPenWidthDp
                                                )
                                            )
                                        }
                                        currentPoints.clear()
                                        isDrawing = false
                                        change.consume()
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }
                    InkTool.ERASER    -> { /* commit 11 */ }
                    InkTool.SELECTION -> { /* commit 12 */ }
                }
                } else Modifier
            )
    ) {
        drawIntoCanvas { canvas ->
            // Draw all committed strokes.
            for (stroke in strokes) {
                if (stroke.points.size < 2) continue
                val widthPx = with(density) { stroke.width.dp.toPx() }
                val paint = Paint().apply {
                    color        = Color(stroke.color.toInt())
                    style        = PaintingStyle.Stroke
                    strokeWidth  = widthPx
                    strokeCap    = StrokeCap.Round
                    strokeJoin   = StrokeJoin.Round
                    isAntiAlias  = true
                }
                canvas.drawPath(buildPath(stroke.points), paint)
            }

            // Draw the in-progress stroke in real time.
            if (isDrawing && currentPoints.size >= 2) {
                val widthPx = with(density) { latestPenWidthDp.dp.toPx() }
                val paint = Paint().apply {
                    color        = Color(latestPenColor.toInt())
                    style        = PaintingStyle.Stroke
                    strokeWidth  = widthPx
                    strokeCap    = StrokeCap.Round
                    strokeJoin   = StrokeJoin.Round
                    isAntiAlias  = true
                }
                canvas.drawPath(buildPath(currentPoints), paint)
            }
        }
    }
}

private fun buildPath(points: List<StrokePoint>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)
    for (i in 1 until points.size) {
        path.lineTo(points[i].x, points[i].y)
    }
    return path
}
