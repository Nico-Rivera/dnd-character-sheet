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
import androidx.compose.ui.geometry.Offset
import kotlin.math.sqrt
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
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
    /** Called on pen-up with the IDs of every stroke erased during the gesture. */
    onErase: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    // Always-current refs — safe to read from a long-lived coroutine without
    // restarting it every time the caller lambda or color/width changes.
    val latestStrokes           by rememberUpdatedState(strokes)
    val latestPenColor          by rememberUpdatedState(penColor)
    val latestPenWidthDp        by rememberUpdatedState(penWidthDp)
    val latestOnStrokeComplete  by rememberUpdatedState(onStrokeComplete)
    val latestOnErase           by rememberUpdatedState(onErase)

    // PEN — in-progress stroke (cleared on each new DOWN event).
    val currentPoints = remember { mutableStateListOf<StrokePoint>() }
    var isDrawing by remember { mutableStateOf(false) }

    // ERASER — tracks touch position for cursor, and IDs erased this gesture.
    var eraserPosition by remember { mutableStateOf<Offset?>(null) }
    val erasedThisGesture = remember { mutableStateListOf<String>() }

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
                                            // Light exponential smoothing to reduce hand tremor
                                            // without killing intentional squiggles. Factor 0.2
                                            // means 80 % of the raw movement is applied each frame.
                                            val raw = change.position
                                            val prev = currentPoints.last()
                                            val sx = prev.x * 0.6f + raw.x * 0.4f
                                            val sy = prev.y * 0.6f + raw.y * 0.4f
                                            currentPoints.add(StrokePoint(sx, sy))
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
                    InkTool.ERASER -> {
                        awaitPointerEventScope {
                            while (true) {
                                val event  = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: continue
                                val pos    = change.position
                                val radiusPx = with(density) { (latestPenWidthDp * 2f).dp.toPx() }
                                when (event.type) {
                                    PointerEventType.Press -> {
                                        erasedThisGesture.clear()
                                        eraserPosition = pos
                                        // Check for hits at the initial touch point.
                                        eraseAt(latestStrokes, erasedThisGesture, pos.x, pos.y, radiusPx)
                                        change.consume()
                                    }
                                    PointerEventType.Move -> {
                                        val prevPos = eraserPosition ?: pos
                                        // Subdivide the sweep so fast swipes don't skip strokes.
                                        // Step size = radius/2 ensures at least one sample covers
                                        // any stroke within the eraser's diameter.
                                        val dx = pos.x - prevPos.x
                                        val dy = pos.y - prevPos.y
                                        val dist = sqrt(dx * dx + dy * dy)
                                        val steps = maxOf(1, (dist / (radiusPx * 0.5f)).toInt())
                                        for (step in 1..steps) {
                                            val t = step.toFloat() / steps
                                            eraseAt(
                                                latestStrokes, erasedThisGesture,
                                                prevPos.x + dx * t, prevPos.y + dy * t,
                                                radiusPx
                                            )
                                        }
                                        eraserPosition = pos
                                        change.consume()
                                    }
                                    PointerEventType.Release -> {
                                        eraserPosition = null
                                        if (erasedThisGesture.isNotEmpty()) {
                                            latestOnErase(erasedThisGesture.toSet())
                                        }
                                        erasedThisGesture.clear()
                                        change.consume()
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }
                    InkTool.SELECTION -> { /* commit 12 */ }
                }
                } else Modifier
            )
    ) {
        drawIntoCanvas { canvas ->
            // Draw committed strokes, skipping any erased during the current gesture.
            for (stroke in strokes) {
                if (stroke.points.size < 2) continue
                if (stroke.id in erasedThisGesture) continue
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

            // Draw the in-progress PEN stroke in real time.
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

        // Eraser cursor — an outline circle showing the erase radius.
        val eraserPos = eraserPosition
        if (activeTool == InkTool.ERASER && eraserPos != null) {
            val radiusPx = with(density) { (penWidthDp * 2f).dp.toPx() }
            drawCircle(
                color  = Color.Gray.copy(alpha = 0.55f),
                radius = radiusPx,
                center = eraserPos,
                style  = DrawStroke(width = 2f)
            )
        }
    }
}

/**
 * Builds a smooth path through [points] using the quadratic Bézier midpoint
 * technique: each recorded point is a control point, and the curve passes
 * through the midpoints between consecutive samples. This eliminates the
 * visible straight-line segments that appear at high draw speed.
 */
private fun buildPath(points: List<StrokePoint>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)
    if (points.size == 1) return path
    for (i in 0 until points.size - 1) {
        val curr = points[i]
        val next = points[i + 1]
        val midX = (curr.x + next.x) * 0.5f
        val midY = (curr.y + next.y) * 0.5f
        path.quadraticBezierTo(curr.x, curr.y, midX, midY)
    }
    // Final segment from the last midpoint to the last point.
    path.lineTo(points.last().x, points.last().y)
    return path
}

/**
 * Adds the IDs of any strokes that intersect (touch, x, y) within [radiusPx]
 * to [erased]. Already-erased IDs are skipped for efficiency.
 */
private fun eraseAt(
    strokes: List<Stroke>,
    erased: MutableList<String>,
    x: Float,
    y: Float,
    radiusPx: Float
) {
    for (stroke in strokes) {
        if (stroke.id in erased) continue
        if (hitsStroke(stroke, x, y, radiusPx)) erased.add(stroke.id)
    }
}

/**
 * Returns true if any part of [stroke]'s path is within [radiusPx] of (x, y).
 * Uses squared-distance comparisons to avoid sqrt on every segment.
 */
private fun hitsStroke(stroke: Stroke, x: Float, y: Float, radiusPx: Float): Boolean {
    val pts = stroke.points
    if (pts.isEmpty()) return false
    val r2 = radiusPx * radiusPx
    if (pts.size == 1) {
        val dx = pts[0].x - x; val dy = pts[0].y - y
        return dx * dx + dy * dy <= r2
    }
    for (i in 0 until pts.size - 1) {
        if (segmentDistSq(pts[i], pts[i + 1], x, y) <= r2) return true
    }
    return false
}

/**
 * Squared distance from point (px, py) to the segment [a]–[b].
 */
private fun segmentDistSq(a: StrokePoint, b: StrokePoint, px: Float, py: Float): Float {
    val dx = b.x - a.x;  val dy = b.y - a.y
    val lenSq = dx * dx + dy * dy
    if (lenSq == 0f) {
        val ex = px - a.x;  val ey = py - a.y
        return ex * ex + ey * ey
    }
    val t = (((px - a.x) * dx + (py - a.y) * dy) / lenSq).coerceIn(0f, 1f)
    val nearX = a.x + t * dx;  val nearY = a.y + t * dy
    val ex = px - nearX;  val ey = py - nearY
    return ex * ex + ey * ey
}
