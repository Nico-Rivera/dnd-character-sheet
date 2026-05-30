package com.dndsheet.app.ui.character.layout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest

// ── Handle geometry constants (canvas pixels) ──────────────────────────────────
private const val SEL_PAD    = 10f  // padding around the selection bounding box
private const val HANDLE_VIS = 8f   // visual radius of corner/rotate handles
private const val ROT_OFFSET = 48f  // rotation handle distance above padded top edge

// Corner hits → proportional resize (opposite corner fixed).
// Edge hits   → single-axis resize (opposite edge fixed).
private enum class HandleHit { NONE, ROTATE, TL, TR, BL, BR, TOP, BOTTOM, LEFT, RIGHT }

// ── Public palette / defaults ──────────────────────────────────────────────────

val INK_PALETTE: List<Int> = listOf(
    0xFF1A1A1A.toInt(),
    0xFFB71C1C.toInt(),
    0xFF1565C0.toInt(),
    0xFF2E7D32.toInt(),
    0xFF6A1B9A.toInt(),
    0xFF4E342E.toInt(),
    0xFFE65100.toInt(),
    0xFFFAFAFA.toInt(),
)
val DEFAULT_INK_COLOR: Long  = INK_PALETTE[0].toLong()
const val DEFAULT_INK_WIDTH: Float = 4f

// ── InkCanvas ──────────────────────────────────────────────────────────────────

/**
 * Transparent overlay that captures touch input and renders ink strokes.
 * Sits above [SheetCanvas] via [Modifier.matchParentSize] + sibling draw order.
 *
 * Tools: PEN, ERASER, SELECTION (rubber-band, move, resize, rotate, copy/cut/paste).
 * When [inkMode] is false no [pointerInput] modifier is added so touches fall
 * through to the sheet boxes for edit-mode drag/resize.
 */
@Composable
fun InkCanvas(
    strokes: List<Stroke>,
    inkMode: Boolean,
    activeTool: InkTool,
    penColor: Long,
    penWidthDp: Float,
    clipboardStrokes: List<Stroke>,
    onStrokeComplete: (Stroke) -> Unit,
    onErase: (Set<String>) -> Unit,
    onSelectionMove: (movedStrokes: List<Stroke>) -> Unit,
    onDelete: (ids: Set<String>) -> Unit,
    onCopy: (strokes: List<Stroke>) -> Unit,
    onCut: (ids: Set<String>) -> Unit,
    onPaste: (toInsert: List<Stroke>) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    // Always-current refs — safe to read from long-lived coroutines.
    val latestStrokes           by rememberUpdatedState(strokes)
    val latestPenColor          by rememberUpdatedState(penColor)
    val latestPenWidthDp        by rememberUpdatedState(penWidthDp)
    val latestClipboard         by rememberUpdatedState(clipboardStrokes)
    val latestOnStrokeComplete  by rememberUpdatedState(onStrokeComplete)
    val latestOnErase           by rememberUpdatedState(onErase)
    val latestOnSelectionMove   by rememberUpdatedState(onSelectionMove)
    val latestOnDelete          by rememberUpdatedState(onDelete)
    val latestOnCopy            by rememberUpdatedState(onCopy)
    val latestOnCut             by rememberUpdatedState(onCut)
    val latestOnPaste           by rememberUpdatedState(onPaste)

    // ── PEN ───────────────────────────────────────────────────────────────────
    val currentPoints = remember { mutableStateListOf<StrokePoint>() }
    var isDrawing by remember { mutableStateOf(false) }

    // ── ERASER ────────────────────────────────────────────────────────────────
    var eraserPosition by remember { mutableStateOf<Offset?>(null) }
    val erasedThisGesture = remember { mutableStateListOf<String>() }

    // ── SELECTION — base ──────────────────────────────────────────────────────
    var selectedIds    by remember { mutableStateOf(emptySet<String>()) }
    var dragStart      by remember { mutableStateOf(Offset.Zero) }
    var rubberBandRect by remember { mutableStateOf<Rect?>(null) }
    var isMoving       by remember { mutableStateOf(false) }
    var moveDelta      by remember { mutableStateOf(Offset.Zero) }

    // ── SELECTION — resize ────────────────────────────────────────────────────
    var isResizing          by remember { mutableStateOf(false) }
    var resizeHandle        by remember { mutableStateOf(HandleHit.NONE) }
    var resizeInitialBounds by remember { mutableStateOf(Rect.Zero) }
    var resizeDelta         by remember { mutableStateOf(Offset.Zero) }

    // ── SELECTION — rotate ────────────────────────────────────────────────────
    var isRotating          by remember { mutableStateOf(false) }
    var rotateInitialBounds by remember { mutableStateOf(Rect.Zero) }
    var rotateInitialAngle  by remember { mutableStateOf(0f) }
    var rotateAngleDelta    by remember { mutableStateOf(0f) }

    // ── SELECTION — long-press paste ──────────────────────────────────────────
    var longPressPastePosition by remember { mutableStateOf<Offset?>(null) }
    val longPressSignal = remember { MutableStateFlow<Offset?>(null) }
    LaunchedEffect(longPressSignal) {
        longPressSignal.collectLatest { pos ->
            if (pos != null) { delay(500L); longPressPastePosition = pos }
        }
    }

    // Clear all selection state when switching away from SELECTION tool.
    LaunchedEffect(activeTool) {
        if (activeTool != InkTool.SELECTION) {
            selectedIds = emptySet()
            rubberBandRect = null
            isMoving = false;    moveDelta = Offset.Zero
            isResizing = false;  resizeDelta = Offset.Zero
            isRotating = false;  rotateAngleDelta = 0f
            longPressPastePosition = null
            longPressSignal.value = null
        }
    }

    // ── Selection bounds (used for handle positions and action bar) ───────────
    // During resize: derived from the scaled initial bounds so handles track the drag.
    // During rotate/move: derived from the transformed stroke points.
    val selBounds: Rect? = run {
        if (activeTool != InkTool.SELECTION || selectedIds.isEmpty()) return@run null
        if (isResizing) {
            return@run resizedBounds(resizeInitialBounds, resizeHandle, resizeDelta)
        }
        val pts = strokes.filter { it.id in selectedIds }.flatMap { s ->
            when {
                isMoving   -> s.points.map { StrokePoint(it.x + moveDelta.x, it.y + moveDelta.y) }
                isRotating -> applyRotateToPoints(s.points, rotateInitialBounds.center, rotateAngleDelta)
                else       -> s.points
            }
        }
        if (pts.isEmpty()) null
        else Rect(pts.minOf { it.x }, pts.minOf { it.y },
                  pts.maxOf { it.x }, pts.maxOf { it.y })
    }

    Box(modifier = modifier) {

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (inkMode) Modifier.pointerInput(activeTool) {
                        val handleHitPx = with(density) { 24.dp.toPx() }

                        // Reset all tool-specific state on every tool change.
                        isDrawing = false;   currentPoints.clear()
                        eraserPosition = null; erasedThisGesture.clear()
                        isResizing = false;  resizeDelta = Offset.Zero
                        isRotating = false;  rotateAngleDelta = 0f

                        when (activeTool) {

                            // ── PEN ───────────────────────────────────────
                            InkTool.PEN -> {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event  = awaitPointerEvent()
                                        val change = event.changes.firstOrNull() ?: continue
                                        when (event.type) {
                                            PointerEventType.Press -> {
                                                currentPoints.clear()
                                                currentPoints.add(StrokePoint(change.position.x, change.position.y))
                                                isDrawing = true
                                                change.consume()
                                            }
                                            PointerEventType.Move -> {
                                                if (isDrawing) {
                                                    val raw  = change.position
                                                    val prev = currentPoints.last()
                                                    currentPoints.add(StrokePoint(
                                                        x = prev.x * 0.6f + raw.x * 0.4f,
                                                        y = prev.y * 0.6f + raw.y * 0.4f
                                                    ))
                                                    change.consume()
                                                }
                                            }
                                            PointerEventType.Release -> {
                                                if (isDrawing && currentPoints.isNotEmpty()) {
                                                    latestOnStrokeComplete(Stroke(
                                                        id     = UUID.randomUUID().toString(),
                                                        points = currentPoints.toList(),
                                                        color  = latestPenColor,
                                                        width  = latestPenWidthDp
                                                    ))
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

                            // ── ERASER ────────────────────────────────────
                            InkTool.ERASER -> {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event    = awaitPointerEvent()
                                        val change   = event.changes.firstOrNull() ?: continue
                                        val pos      = change.position
                                        val radiusPx = with(density) { (latestPenWidthDp * 2f).dp.toPx() }
                                        when (event.type) {
                                            PointerEventType.Press -> {
                                                erasedThisGesture.clear()
                                                eraserPosition = pos
                                                eraseAt(latestStrokes, erasedThisGesture, pos.x, pos.y, radiusPx)
                                                change.consume()
                                            }
                                            PointerEventType.Move -> {
                                                val prevPos = eraserPosition ?: pos
                                                val dx   = pos.x - prevPos.x
                                                val dy   = pos.y - prevPos.y
                                                val dist = sqrt(dx * dx + dy * dy)
                                                val steps = maxOf(1, (dist / (radiusPx * 0.5f)).toInt())
                                                for (step in 1..steps) {
                                                    val t = step.toFloat() / steps
                                                    eraseAt(latestStrokes, erasedThisGesture,
                                                        prevPos.x + dx * t, prevPos.y + dy * t, radiusPx)
                                                }
                                                eraserPosition = pos
                                                change.consume()
                                            }
                                            PointerEventType.Release -> {
                                                eraserPosition = null
                                                if (erasedThisGesture.isNotEmpty()) latestOnErase(erasedThisGesture.toSet())
                                                erasedThisGesture.clear()
                                                change.consume()
                                            }
                                            else -> {}
                                        }
                                    }
                                }
                            }

                            // ── SELECTION ─────────────────────────────────
                            InkTool.SELECTION -> {
                                var longPressLeftBuffer = false
                                val bufferPx = with(density) { 20.dp.toPx() }

                                awaitPointerEventScope {
                                    while (true) {
                                        val event  = awaitPointerEvent()
                                        val change = event.changes.firstOrNull() ?: continue
                                        val pos    = change.position
                                        when (event.type) {

                                            PointerEventType.Press -> {
                                                longPressPastePosition = null
                                                longPressSignal.value  = null
                                                longPressLeftBuffer    = false

                                                val bounds = selectionBoundsOf(latestStrokes, selectedIds)
                                                if (selectedIds.isNotEmpty() && bounds != null) {
                                                    when (val hit = hitTestHandle(pos, bounds, handleHitPx)) {
                                                        HandleHit.ROTATE -> {
                                                            isRotating = true
                                                            rotateInitialBounds = bounds
                                                            rotateInitialAngle  = atan2(pos.y - bounds.center.y,
                                                                                        pos.x - bounds.center.x)
                                                            rotateAngleDelta = 0f
                                                        }
                                                        HandleHit.TL, HandleHit.TR,
                                                        HandleHit.BL, HandleHit.BR,
                                                        HandleHit.TOP, HandleHit.BOTTOM,
                                                        HandleHit.LEFT, HandleHit.RIGHT -> {
                                                            isResizing = true
                                                            resizeHandle        = hit
                                                            resizeInitialBounds = bounds
                                                            dragStart           = pos
                                                            resizeDelta         = Offset.Zero
                                                        }
                                                        HandleHit.NONE -> {
                                                            if (bounds.inflate(24f).contains(pos)) {
                                                                isMoving  = true
                                                                dragStart = pos
                                                                moveDelta = Offset.Zero
                                                            } else {
                                                                isMoving = false; isResizing = false; isRotating = false
                                                                selectedIds    = emptySet()
                                                                dragStart      = pos
                                                                rubberBandRect = Rect(pos.x, pos.y, pos.x, pos.y)
                                                                if (latestClipboard.isNotEmpty()) longPressSignal.value = pos
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    isMoving = false; isResizing = false; isRotating = false
                                                    selectedIds    = emptySet()
                                                    dragStart      = pos
                                                    rubberBandRect = Rect(pos.x, pos.y, pos.x, pos.y)
                                                    if (latestClipboard.isNotEmpty()) longPressSignal.value = pos
                                                }
                                                change.consume()
                                            }

                                            PointerEventType.Move -> {
                                                when {
                                                    isMoving -> moveDelta = pos - dragStart
                                                    isResizing -> resizeDelta = pos - dragStart
                                                    isRotating -> {
                                                        val c = rotateInitialBounds.center
                                                        rotateAngleDelta = atan2(pos.y - c.y, pos.x - c.x) - rotateInitialAngle
                                                    }
                                                    else -> {
                                                        if (!longPressLeftBuffer) {
                                                            val dx = pos.x - dragStart.x
                                                            val dy = pos.y - dragStart.y
                                                            if (dx * dx + dy * dy > bufferPx * bufferPx) {
                                                                longPressLeftBuffer   = true
                                                                longPressSignal.value = null
                                                            }
                                                        }
                                                        rubberBandRect = Rect(
                                                            left   = minOf(dragStart.x, pos.x),
                                                            top    = minOf(dragStart.y, pos.y),
                                                            right  = maxOf(dragStart.x, pos.x),
                                                            bottom = maxOf(dragStart.y, pos.y)
                                                        )
                                                    }
                                                }
                                                change.consume()
                                            }

                                            PointerEventType.Release -> {
                                                longPressSignal.value = null
                                                when {
                                                    isMoving -> {
                                                        val delta = moveDelta
                                                        val moved = latestStrokes
                                                            .filter { it.id in selectedIds }
                                                            .map { s -> s.copy(points = s.points.map { pt ->
                                                                StrokePoint(pt.x + delta.x, pt.y + delta.y)
                                                            })}
                                                        if (moved.isNotEmpty()) latestOnSelectionMove(moved)
                                                        isMoving = false; moveDelta = Offset.Zero
                                                    }
                                                    isResizing -> {
                                                        val transformed = latestStrokes
                                                            .filter { it.id in selectedIds }
                                                            .map { s -> s.copy(points = applyResizeToPoints(
                                                                s.points, resizeInitialBounds, resizeHandle, resizeDelta)) }
                                                        if (transformed.isNotEmpty()) latestOnSelectionMove(transformed)
                                                        isResizing = false; resizeHandle = HandleHit.NONE; resizeDelta = Offset.Zero
                                                    }
                                                    isRotating -> {
                                                        val c = rotateInitialBounds.center
                                                        val angle = rotateAngleDelta
                                                        val transformed = latestStrokes
                                                            .filter { it.id in selectedIds }
                                                            .map { s -> s.copy(points = applyRotateToPoints(s.points, c, angle)) }
                                                        if (transformed.isNotEmpty()) latestOnSelectionMove(transformed)
                                                        isRotating = false; rotateAngleDelta = 0f
                                                    }
                                                    else -> {
                                                        val rect = rubberBandRect
                                                        rubberBandRect = null
                                                        val newSel = if (rect != null && rect.width > 8f && rect.height > 8f)
                                                            latestStrokes.filter { strokeIntersectsRect(it, rect) }.map { it.id }.toSet()
                                                        else emptySet()
                                                        selectedIds = newSel
                                                        if (newSel.isNotEmpty()) longPressPastePosition = null
                                                    }
                                                }
                                                change.consume()
                                            }
                                            else -> {}
                                        }
                                    }
                                }
                            }
                        }
                    } else Modifier
                )
        ) {
            drawIntoCanvas { canvas ->

                // ── Committed strokes (with live transform preview) ────────────
                for (stroke in strokes) {
                    if (stroke.points.size < 2) continue
                    if (stroke.id in erasedThisGesture) continue
                    val isSelected = stroke.id in selectedIds
                    val pts: List<StrokePoint> = when {
                        isMoving   && isSelected ->
                            stroke.points.map { StrokePoint(it.x + moveDelta.x, it.y + moveDelta.y) }
                        isResizing && isSelected ->
                            applyResizeToPoints(stroke.points, resizeInitialBounds, resizeHandle, resizeDelta)
                        isRotating && isSelected ->
                            applyRotateToPoints(stroke.points, rotateInitialBounds.center, rotateAngleDelta)
                        else -> stroke.points
                    }
                    val widthPx = with(density) { stroke.width.dp.toPx() }
                    canvas.drawPath(buildPath(pts), Paint().apply {
                        color = if (isSelected && activeTool == InkTool.SELECTION)
                            Color(stroke.color.toInt()).copy(alpha = 0.55f)
                        else
                            Color(stroke.color.toInt())
                        style       = PaintingStyle.Stroke
                        strokeWidth = widthPx
                        strokeCap   = StrokeCap.Round
                        strokeJoin  = StrokeJoin.Round
                        isAntiAlias = true
                    })
                }

                // ── In-progress PEN stroke ─────────────────────────────────────
                if (isDrawing && currentPoints.size >= 2) {
                    canvas.drawPath(buildPath(currentPoints), Paint().apply {
                        color       = Color(latestPenColor.toInt())
                        style       = PaintingStyle.Stroke
                        strokeWidth = with(density) { latestPenWidthDp.dp.toPx() }
                        strokeCap   = StrokeCap.Round
                        strokeJoin  = StrokeJoin.Round
                        isAntiAlias = true
                    })
                }

                // ── Rubber-band rect ───────────────────────────────────────────
                rubberBandRect?.let { rect ->
                    canvas.drawPath(rectPath(rect), Paint().apply {
                        color = Color(0xFF1565C0.toInt()).copy(alpha = 0.08f)
                        style = PaintingStyle.Fill
                    })
                    canvas.drawPath(rectPath(rect), Paint().apply {
                        color       = Color(0xFF1565C0.toInt()).copy(alpha = 0.7f)
                        style       = PaintingStyle.Stroke
                        strokeWidth = 2f
                        pathEffect  = PathEffect.dashPathEffect(floatArrayOf(8f, 4f))
                    })
                }

                // ── Selection bounding box + transform handles ─────────────────
                // Handles are only drawn when idle (no active transform gesture),
                // so the user always sees a clean preview during drag.
                val showHandles = activeTool == InkTool.SELECTION &&
                    selectedIds.isNotEmpty() && !isMoving && !isResizing && !isRotating
                if (activeTool == InkTool.SELECTION && selectedIds.isNotEmpty()) {
                    selBounds?.let { b ->
                        // Dashed bounding box
                        canvas.drawPath(
                            rectPath(Rect(b.left - SEL_PAD, b.top - SEL_PAD,
                                          b.right + SEL_PAD, b.bottom + SEL_PAD)),
                            Paint().apply {
                                color       = Color(0xFF1565C0.toInt()).copy(alpha = 0.85f)
                                style       = PaintingStyle.Stroke
                                strokeWidth = 2f
                                pathEffect  = PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
                            }
                        )

                        if (showHandles) {
                            val handleFill = Paint().apply {
                                color = Color(0xFF1565C0.toInt()); style = PaintingStyle.Fill
                            }
                            val handleRing = Paint().apply {
                                color = Color.White; style = PaintingStyle.Stroke; strokeWidth = 2f
                            }

                            // Corner handles — filled squares (proportional resize)
                            for (corner in listOf(
                                Offset(b.left  - SEL_PAD, b.top    - SEL_PAD),
                                Offset(b.right + SEL_PAD, b.top    - SEL_PAD),
                                Offset(b.left  - SEL_PAD, b.bottom + SEL_PAD),
                                Offset(b.right + SEL_PAD, b.bottom + SEL_PAD)
                            )) {
                                val r = Rect(corner.x - HANDLE_VIS, corner.y - HANDLE_VIS,
                                             corner.x + HANDLE_VIS, corner.y + HANDLE_VIS)
                                canvas.drawPath(rectPath(r), handleFill)
                                canvas.drawPath(rectPath(r), handleRing)
                            }

                            // Edge handles — filled circles (single-axis resize)
                            val edgeR = HANDLE_VIS * 0.75f
                            for (edge in listOf(
                                Offset(b.center.x, b.top    - SEL_PAD),  // top
                                Offset(b.center.x, b.bottom + SEL_PAD),  // bottom
                                Offset(b.left  - SEL_PAD, b.center.y),   // left
                                Offset(b.right + SEL_PAD, b.center.y)    // right
                            )) {
                                canvas.drawCircle(edge, edgeR, handleFill)
                                canvas.drawCircle(edge, edgeR, handleRing)
                            }

                            // Rotation handle — circle above top-center + connecting line
                            val rotHandle = Offset(b.center.x, b.top - SEL_PAD - ROT_OFFSET)
                            canvas.drawPath(Path().apply {
                                moveTo(b.center.x, b.top - SEL_PAD)
                                lineTo(rotHandle.x, rotHandle.y)
                            }, Paint().apply {
                                color = Color(0xFF1565C0.toInt()).copy(alpha = 0.6f)
                                style = PaintingStyle.Stroke; strokeWidth = 1.5f
                            })
                            canvas.drawCircle(rotHandle, HANDLE_VIS, handleFill)
                            canvas.drawCircle(rotHandle, HANDLE_VIS, handleRing)
                        }
                    }
                }
            } // end drawIntoCanvas

            // ── Eraser cursor ──────────────────────────────────────────────────
            val eraserPos = eraserPosition
            if (activeTool == InkTool.ERASER && eraserPos != null) {
                drawCircle(
                    color  = Color.Gray.copy(alpha = 0.55f),
                    radius = with(density) { (penWidthDp * 2f).dp.toPx() },
                    center = eraserPos,
                    style  = DrawStroke(width = 2f)
                )
            }
        } // end Canvas

        // ── Rotation angle readout ─────────────────────────────────────────────
        if (isRotating && selBounds != null) {
            val angleDeg = (rotateAngleDelta * 180.0 / Math.PI).toInt()
            val rotHandle = Offset(selBounds!!.center.x, selBounds!!.top - SEL_PAD - ROT_OFFSET)
            Text(
                text  = "$angleDeg°",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF1565C0.toInt()),
                modifier = Modifier.offset(
                    x = with(density) { (rotHandle.x + HANDLE_VIS + 4f).toDp() },
                    y = with(density) { (rotHandle.y - 8f).toDp() }
                )
            )
        }

        // ── Long-press paste chip ──────────────────────────────────────────────
        longPressPastePosition?.let { pressPos ->
            Surface(
                modifier        = Modifier.offset(
                    x = with(density) { pressPos.x.toDp() },
                    y = with(density) { pressPos.y.toDp() }
                ),
                shape           = RoundedCornerShape(8.dp),
                tonalElevation  = 6.dp,
                shadowElevation = 4.dp
            ) {
                IconButton(
                    onClick  = { latestOnPaste(pasteAt(latestClipboard, pressPos)); longPressPastePosition = null },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste",
                         modifier = Modifier.size(20.dp))
                }
            }
        }

        // ── Selection action bar ───────────────────────────────────────────────
        if (activeTool == InkTool.SELECTION && selectedIds.isNotEmpty()) {
            selBounds?.let { bounds ->
                val xDp = with(density) { bounds.left.toDp() }
                val yDp = with(density) { (bounds.bottom + SEL_PAD + 6f).toDp() }
                SelectionActionBar(
                    modifier  = Modifier.offset(x = xDp, y = yDp),
                    showPaste = clipboardStrokes.isNotEmpty(),
                    onDelete  = { latestOnDelete(selectedIds); selectedIds = emptySet() },
                    onCopy    = { latestOnCopy(strokes.filter { it.id in selectedIds }) },
                    onCut     = { latestOnCut(selectedIds); selectedIds = emptySet() },
                    onPaste   = {
                        val center = Offset((bounds.left + bounds.right) / 2f,
                                            (bounds.top  + bounds.bottom) / 2f)
                        latestOnPaste(pasteAt(latestClipboard, center))
                    }
                )
            }
        }

    } // end Box
}

// ── SelectionActionBar ─────────────────────────────────────────────────────────

@Composable
private fun SelectionActionBar(
    showPaste: Boolean,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onPaste: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier        = modifier,
        shape           = RoundedCornerShape(8.dp),
        tonalElevation  = 6.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete,      contentDescription = "Delete",  modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onCopy, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy",    modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onCut, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ContentCut,  contentDescription = "Cut",     modifier = Modifier.size(18.dp))
            }
            if (showPaste) {
                IconButton(onClick = onPaste, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ── Transform helpers ──────────────────────────────────────────────────────────

/**
 * Applies the appropriate resize transform to [points] based on [handle].
 *
 * Corner handles (TL/TR/BL/BR): proportional (uniform) scale anchored at the
 * opposite corner. The scale factor is derived by projecting the drag delta
 * onto the original corner's diagonal direction so the resize feels natural
 * at any drag angle.
 *
 * Edge handles (TOP/BOTTOM/LEFT/RIGHT): single-axis scale anchored at the
 * opposite edge. The perpendicular axis is unchanged.
 */
private fun applyResizeToPoints(
    points: List<StrokePoint>, bounds: Rect, handle: HandleHit, delta: Offset
): List<StrokePoint> {
    if (points.isEmpty()) return points
    return when (handle) {
        // ── Proportional corner resize ─────────────────────────────────────
        HandleHit.TL, HandleHit.TR, HandleHit.BL, HandleHit.BR -> {
            val (anchor, origCorner) = cornerAnchorAndPos(bounds, handle)
            val origVec  = origCorner - anchor
            val origDiag = sqrt(origVec.x * origVec.x + origVec.y * origVec.y)
            if (origDiag < 1f) return points
            val diagDir   = Offset(origVec.x / origDiag, origVec.y / origDiag)
            val projected = delta.x * diagDir.x + delta.y * diagDir.y
            val scale     = ((origDiag + projected) / origDiag).coerceAtLeast(0.05f)
            points.map { StrokePoint(anchor.x + (it.x - anchor.x) * scale,
                                     anchor.y + (it.y - anchor.y) * scale) }
        }
        // ── Single-axis edge resize ────────────────────────────────────────
        HandleHit.LEFT -> {
            val ax = bounds.right
            val sx = ((bounds.width - delta.x) / bounds.width).coerceAtLeast(0.05f)
            points.map { StrokePoint(ax + (it.x - ax) * sx, it.y) }
        }
        HandleHit.RIGHT -> {
            val ax = bounds.left
            val sx = ((bounds.width + delta.x) / bounds.width).coerceAtLeast(0.05f)
            points.map { StrokePoint(ax + (it.x - ax) * sx, it.y) }
        }
        HandleHit.TOP -> {
            val ay = bounds.bottom
            val sy = ((bounds.height - delta.y) / bounds.height).coerceAtLeast(0.05f)
            points.map { StrokePoint(it.x, ay + (it.y - ay) * sy) }
        }
        HandleHit.BOTTOM -> {
            val ay = bounds.top
            val sy = ((bounds.height + delta.y) / bounds.height).coerceAtLeast(0.05f)
            points.map { StrokePoint(it.x, ay + (it.y - ay) * sy) }
        }
        else -> points
    }
}

/** Returns the new bounding box of the selection after a resize gesture. */
private fun resizedBounds(bounds: Rect, handle: HandleHit, delta: Offset): Rect {
    return when (handle) {
        HandleHit.LEFT   -> Rect(bounds.left + delta.x, bounds.top, bounds.right, bounds.bottom)
        HandleHit.RIGHT  -> Rect(bounds.left, bounds.top, bounds.right + delta.x, bounds.bottom)
        HandleHit.TOP    -> Rect(bounds.left, bounds.top + delta.y, bounds.right, bounds.bottom)
        HandleHit.BOTTOM -> Rect(bounds.left, bounds.top, bounds.right, bounds.bottom + delta.y)
        HandleHit.TL, HandleHit.TR, HandleHit.BL, HandleHit.BR -> {
            val (anchor, origCorner) = cornerAnchorAndPos(bounds, handle)
            val origVec  = origCorner - anchor
            val origDiag = sqrt(origVec.x * origVec.x + origVec.y * origVec.y)
            if (origDiag < 1f) return bounds
            val diagDir   = Offset(origVec.x / origDiag, origVec.y / origDiag)
            val projected = delta.x * diagDir.x + delta.y * diagDir.y
            val scale     = ((origDiag + projected) / origDiag).coerceAtLeast(0.05f)
            // Scale the entire original bounding box from the anchor point
            Rect(
                left   = anchor.x + (bounds.left   - anchor.x) * scale,
                top    = anchor.y + (bounds.top    - anchor.y) * scale,
                right  = anchor.x + (bounds.right  - anchor.x) * scale,
                bottom = anchor.y + (bounds.bottom - anchor.y) * scale
            )
        }
        else -> bounds
    }
}

/** Anchor (fixed corner) and original draggable corner position for a corner handle. */
private fun cornerAnchorAndPos(bounds: Rect, handle: HandleHit): Pair<Offset, Offset> = when (handle) {
    HandleHit.BR -> Pair(Offset(bounds.left,  bounds.top),    Offset(bounds.right,  bounds.bottom))
    HandleHit.TL -> Pair(Offset(bounds.right, bounds.bottom), Offset(bounds.left,   bounds.top))
    HandleHit.TR -> Pair(Offset(bounds.left,  bounds.bottom), Offset(bounds.right,  bounds.top))
    HandleHit.BL -> Pair(Offset(bounds.right, bounds.top),    Offset(bounds.left,   bounds.bottom))
    else         -> Pair(Offset.Zero, Offset.Zero)
}

private fun applyRotateToPoints(
    points: List<StrokePoint>, center: Offset, angleDelta: Float
): List<StrokePoint> {
    val cosA = cos(angleDelta); val sinA = sin(angleDelta)
    return points.map { pt ->
        val dx = pt.x - center.x; val dy = pt.y - center.y
        StrokePoint(center.x + dx * cosA - dy * sinA,
                    center.y + dx * sinA + dy * cosA)
    }
}

/**
 * Hit-tests the 5 transform handles (4 corners + rotation) against [pos].
 * [hitPx] is the touch radius (24 dp → 48 dp diameter, meeting Material minimum).
 */
private fun hitTestHandle(pos: Offset, bounds: Rect, hitPx: Float): HandleHit {
    fun d2(a: Offset, b: Offset): Float { val dx = a.x-b.x; val dy = a.y-b.y; return dx*dx+dy*dy }
    val r2 = hitPx * hitPx
    // Rotation handle tested first (it's outside the box so no ambiguity)
    if (d2(pos, Offset(bounds.center.x, bounds.top - SEL_PAD - ROT_OFFSET)) <= r2) return HandleHit.ROTATE
    // Corners (proportional resize) — tested before edges so corners win at intersections
    listOf(
        HandleHit.TL to Offset(bounds.left  - SEL_PAD, bounds.top    - SEL_PAD),
        HandleHit.TR to Offset(bounds.right + SEL_PAD, bounds.top    - SEL_PAD),
        HandleHit.BL to Offset(bounds.left  - SEL_PAD, bounds.bottom + SEL_PAD),
        HandleHit.BR to Offset(bounds.right + SEL_PAD, bounds.bottom + SEL_PAD)
    ).forEach { (hit, p) -> if (d2(pos, p) <= r2) return hit }
    // Edge handles (single-axis resize)
    listOf(
        HandleHit.TOP    to Offset(bounds.center.x, bounds.top    - SEL_PAD),
        HandleHit.BOTTOM to Offset(bounds.center.x, bounds.bottom + SEL_PAD),
        HandleHit.LEFT   to Offset(bounds.left  - SEL_PAD, bounds.center.y),
        HandleHit.RIGHT  to Offset(bounds.right + SEL_PAD, bounds.center.y)
    ).forEach { (hit, p) -> if (d2(pos, p) <= r2) return hit }
    return HandleHit.NONE
}

// ── Drawing helpers ────────────────────────────────────────────────────────────

private fun rectPath(rect: Rect): Path = Path().apply {
    moveTo(rect.left, rect.top); lineTo(rect.right, rect.top)
    lineTo(rect.right, rect.bottom); lineTo(rect.left, rect.bottom); close()
}

private fun buildPath(points: List<StrokePoint>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)
    if (points.size == 1) return path
    for (i in 0 until points.size - 1) {
        val curr = points[i]; val next = points[i + 1]
        path.quadraticBezierTo(curr.x, curr.y, (curr.x+next.x)*0.5f, (curr.y+next.y)*0.5f)
    }
    path.lineTo(points.last().x, points.last().y)
    return path
}

private fun pasteAt(strokes: List<Stroke>, target: Offset): List<Stroke> {
    val allPts = strokes.flatMap { it.points }
    if (allPts.isEmpty()) return emptyList()
    val cx = (allPts.minOf { it.x } + allPts.maxOf { it.x }) / 2f
    val cy = (allPts.minOf { it.y } + allPts.maxOf { it.y }) / 2f
    val dx = target.x - cx; val dy = target.y - cy
    return strokes.map { s ->
        s.copy(id = UUID.randomUUID().toString(),
               points = s.points.map { StrokePoint(it.x + dx, it.y + dy) })
    }
}

// ── Hit-detection helpers ──────────────────────────────────────────────────────

private fun selectionBoundsOf(strokes: List<Stroke>, ids: Set<String>): Rect? {
    val pts = strokes.filter { it.id in ids }.flatMap { it.points }
    if (pts.isEmpty()) return null
    return Rect(pts.minOf { it.x }, pts.minOf { it.y }, pts.maxOf { it.x }, pts.maxOf { it.y })
}

private fun strokeIntersectsRect(stroke: Stroke, rect: Rect): Boolean {
    val pts = stroke.points
    if (pts.isEmpty()) return false
    if (pts.any { rect.contains(Offset(it.x, it.y)) }) return true
    if (pts.size < 2) return false
    val edges = listOf(rect.topLeft to rect.topRight, rect.topRight to rect.bottomRight,
                       rect.bottomRight to rect.bottomLeft, rect.bottomLeft to rect.topLeft)
    for (i in 0 until pts.size - 1) {
        val a = Offset(pts[i].x, pts[i].y); val b = Offset(pts[i+1].x, pts[i+1].y)
        if (edges.any { (c, d) -> segmentsIntersect(a, b, c, d) }) return true
    }
    return false
}

private fun segmentsIntersect(a: Offset, b: Offset, c: Offset, d: Offset): Boolean {
    fun cross(o: Offset, u: Offset, v: Offset) = (u.x-o.x)*(v.y-o.y) - (u.y-o.y)*(v.x-o.x)
    val d1 = cross(c,d,a); val d2 = cross(c,d,b)
    val d3 = cross(a,b,c); val d4 = cross(a,b,d)
    return ((d1>0&&d2<0)||(d1<0&&d2>0)) && ((d3>0&&d4<0)||(d3<0&&d4>0))
}

private fun eraseAt(strokes: List<Stroke>, erased: MutableList<String>, x: Float, y: Float, radiusPx: Float) {
    for (stroke in strokes) {
        if (stroke.id in erased) continue
        if (hitsStroke(stroke, x, y, radiusPx)) erased.add(stroke.id)
    }
}

private fun hitsStroke(stroke: Stroke, x: Float, y: Float, radiusPx: Float): Boolean {
    val pts = stroke.points; if (pts.isEmpty()) return false
    val r2 = radiusPx * radiusPx
    if (pts.size == 1) { val dx = pts[0].x-x; val dy = pts[0].y-y; return dx*dx+dy*dy <= r2 }
    for (i in 0 until pts.size-1) { if (segmentDistSq(pts[i], pts[i+1], x, y) <= r2) return true }
    return false
}

private fun segmentDistSq(a: StrokePoint, b: StrokePoint, px: Float, py: Float): Float {
    val dx = b.x-a.x; val dy = b.y-a.y; val lenSq = dx*dx+dy*dy
    if (lenSq == 0f) { val ex = px-a.x; val ey = py-a.y; return ex*ex+ey*ey }
    val t = (((px-a.x)*dx+(py-a.y)*dy)/lenSq).coerceIn(0f,1f)
    val ex = px-(a.x+t*dx); val ey = py-(a.y+t*dy); return ex*ex+ey*ey
}

