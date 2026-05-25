package com.dndsheet.app.ui.character.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.dndsheet.domain.enums.Ruleset
import com.dndsheet.domain.model.BoxPosition

/** Global minimum box dimensions enforced during resize (dp values). */
const val MIN_BOX_WIDTH_DP  = 80f
const val MIN_BOX_HEIGHT_DP = 48f

/** Depth of the edge/corner touch zone that triggers a resize gesture (dp). */
private const val EDGE_ZONE_DP = 28f

private val HANDLE_THICK  = 4.dp   // narrow axis of edge handle bars
private val HANDLE_LONG   = 28.dp  // long axis of edge handle bars
private val CORNER_SIZE   = 12.dp  // side of the corner square handle

/** Which kind of drag is in progress on an [EditableSheetBox]. */
private enum class DragMode { Move, RightEdge, BottomEdge, Corner }

/**
 * Stable identifiers for every sheet box. The enum `name` is persisted in
 * [com.dndsheet.domain.model.SheetLayout.positions] — never rename entries
 * or saved positions will be orphaned. Ruleset-specific boxes all live here;
 * the canvas ignores ids that have no matching child for the active ruleset.
 */
enum class BoxId {
    HEADER,
    VITALS_LEVEL, VITALS_PROF, VITALS_INIT, VITALS_HP,
    HIT_DICE,
    ABILITY_STR, ABILITY_DEX, ABILITY_CON, ABILITY_INT, ABILITY_WIS, ABILITY_CHA,
    SAVES_ALL,
    SAVE_STR, SAVE_DEX, SAVE_CON, SAVE_INT, SAVE_WIS, SAVE_CHA,
    SKILLS_ALL,
    SKILLS_STR, SKILLS_DEX, SKILLS_INT, SKILLS_WIS, SKILLS_CHA,
    PASSIVES,
    CONDITIONS
}

/**
 * The default arrangement of boxes, as rows of [BoxId]s. Boxes in the same
 * row sit side by side; rows stack top to bottom. The canvas collapses any
 * row whose ids have no matching child for the active ruleset.
 */
fun defaultRows(ruleset: Ruleset): List<List<BoxId>> = buildList {
    add(listOf(BoxId.HEADER))
    add(listOf(BoxId.VITALS_LEVEL, BoxId.VITALS_PROF, BoxId.VITALS_INIT, BoxId.VITALS_HP))
    add(listOf(BoxId.HIT_DICE))
    add(listOf(BoxId.ABILITY_STR, BoxId.ABILITY_DEX, BoxId.ABILITY_CON))
    add(listOf(BoxId.ABILITY_INT, BoxId.ABILITY_WIS, BoxId.ABILITY_CHA))
    if (ruleset == Ruleset.DND_5E_2014) {
        add(listOf(BoxId.SAVES_ALL))
        add(listOf(BoxId.SKILLS_ALL))
    } else {
        add(listOf(BoxId.SAVE_STR, BoxId.SAVE_DEX, BoxId.SAVE_CON))
        add(listOf(BoxId.SAVE_INT, BoxId.SAVE_WIS, BoxId.SAVE_CHA))
        add(listOf(BoxId.SKILLS_STR))
        add(listOf(BoxId.SKILLS_DEX))
        add(listOf(BoxId.SKILLS_INT))
        add(listOf(BoxId.SKILLS_WIS))
        add(listOf(BoxId.SKILLS_CHA))
    }
    add(listOf(BoxId.PASSIVES))
    add(listOf(BoxId.CONDITIONS))
}

/**
 * Wraps a sheet-box composable, tagging it with [boxId] for [SheetCanvas] to
 * find, and — in edit mode — enabling drag-to-move, edge/corner resize, and
 * z-order step buttons.
 *
 * **Gesture architecture**: a *single* [pointerInput] on the root [Box]
 * handles all drag interactions. On [onDragStart] it classifies the initial
 * touch against the [EDGE_ZONE_DP] bands along the right and bottom edges:
 *
 * - Touch in right zone only  → [DragMode.RightEdge]  (width resize)
 * - Touch in bottom zone only → [DragMode.BottomEdge] (height resize)
 * - Touch in both zones       → [DragMode.Corner]     (both axes)
 * - Touch elsewhere           → [DragMode.Move]        (reposition)
 *
 * Using one [pointerInput] eliminates the competing-detectors problem that
 * would occur if the visual handle overlays had their own gesture sources.
 *
 * **Width/height semantics in [onResize]**: `null` means "keep current stored
 * value." Right-edge drag passes `(newWidth, null)` so height stays auto;
 * bottom-edge passes `(null, newHeight)`; corner passes both non-null.
 *
 * **Z-order buttons**: small ▲/▼ buttons rendered at the top-end corner.
 * Clicks don't conflict with the drag detector (no slop threshold is reached
 * by a tap, so [detectDragGestures] stays idle for taps).
 */
@Composable
fun EditableSheetBox(
    boxId: BoxId,
    editing: Boolean,
    onMove: (BoxId, xDp: Float, yDp: Float) -> Unit,
    onResize: (BoxId, widthDp: Float?, heightDp: Float?) -> Unit,
    onZChange: (BoxId, delta: Int) -> Unit,
    onCommit: () -> Unit,
    content: @Composable () -> Unit
) {
    val density    = LocalDensity.current
    val posInParent = remember { mutableStateOf(Offset.Zero) }
    val sizePx      = remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = Modifier
            .layoutId(boxId)
            .onGloballyPositioned { posInParent.value = it.positionInParent() }
            .onSizeChanged { sizePx.value = it }
            .pointerInput(boxId, editing) {
                if (!editing) return@pointerInput
                val edgeZonePx = with(density) { EDGE_ZONE_DP.dp.toPx() }
                var mode = DragMode.Move
                // Accumulated move position (dp)
                var sx = 0f; var sy = 0f
                // Accumulated resize dimensions (dp)
                var sw = 0f; var sh = 0f

                detectDragGestures(
                    onDragStart = { offset ->
                        val sz = sizePx.value
                        val inRight  = offset.x > sz.width  - edgeZonePx
                        val inBottom = offset.y > sz.height - edgeZonePx
                        mode = when {
                            inRight && inBottom -> DragMode.Corner
                            inRight             -> DragMode.RightEdge
                            inBottom            -> DragMode.BottomEdge
                            else                -> DragMode.Move
                        }
                        when (mode) {
                            DragMode.Move -> {
                                sx = with(density) { posInParent.value.x.toDp().value }
                                sy = with(density) { posInParent.value.y.toDp().value }
                                onMove(boxId, sx, sy)
                            }
                            else -> {
                                sw = with(density) { sz.width.toDp().value }
                                sh = with(density) { sz.height.toDp().value }
                                // Pin the current canvas position before we start writing
                                // width/height. Without this, a box that has no stored
                                // position defaults to BoxPosition() (x=0, y=0), which
                                // teleports it to the canvas origin on the first resize frame.
                                val px = with(density) { posInParent.value.x.toDp().value }
                                val py = with(density) { posInParent.value.y.toDp().value }
                                onMove(boxId, px, py)
                            }
                        }
                    },
                    onDrag = { change, drag ->
                        change.consume()
                        val dx = with(density) { drag.x.toDp().value }
                        val dy = with(density) { drag.y.toDp().value }
                        when (mode) {
                            DragMode.Move -> {
                                sx = (sx + dx).coerceAtLeast(0f)
                                sy = (sy + dy).coerceAtLeast(0f)
                                onMove(boxId, sx, sy)
                            }
                            DragMode.RightEdge -> {
                                sw = (sw + dx).coerceAtLeast(MIN_BOX_WIDTH_DP)
                                onResize(boxId, sw, null)
                            }
                            DragMode.BottomEdge -> {
                                sh = (sh + dy).coerceAtLeast(MIN_BOX_HEIGHT_DP)
                                onResize(boxId, null, sh)
                            }
                            DragMode.Corner -> {
                                sw = (sw + dx).coerceAtLeast(MIN_BOX_WIDTH_DP)
                                sh = (sh + dy).coerceAtLeast(MIN_BOX_HEIGHT_DP)
                                onResize(boxId, sw, sh)
                            }
                        }
                    },
                    onDragEnd    = { onCommit() },
                    onDragCancel = { onCommit() }
                )
            }
    ) {
        // When SheetCanvas gives this Box a fixed height (stored resize value),
        // Box propagates minHeight=0 to children, so the inner composable wraps
        // at its natural height instead of filling the box. This Layout re-applies
        // the tight lower bound so the content stretches to fill the stored height.
        // When height is auto (maxHeight = Infinity, hasBoundedHeight = false),
        // minHeight stays 0 and the content wraps exactly as before.
        Layout(content = { content() }) { measurables, constraints ->
            val fillH = if (constraints.hasBoundedHeight) constraints.maxHeight else 0
            val placeables = measurables.map {
                it.measure(constraints.copy(minHeight = fillH))
            }
            val h = placeables.maxOfOrNull { it.height } ?: 0
            layout(constraints.maxWidth, h) {
                placeables.forEach { it.place(0, 0) }
            }
        }

        if (editing) {
            val handleColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)

            // Z-order step buttons — top-end corner, small and unobtrusive
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                ZButton(Icons.Default.KeyboardArrowUp)   { onZChange(boxId, +1) }
                ZButton(Icons.Default.KeyboardArrowDown) { onZChange(boxId, -1) }
            }

            // Right-edge handle (visual cue for width resize zone)
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .width(HANDLE_THICK)
                    .height(HANDLE_LONG)
                    .background(handleColor, RoundedCornerShape(2.dp))
            )

            // Bottom-edge handle (visual cue for height resize zone)
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .width(HANDLE_LONG)
                    .height(HANDLE_THICK)
                    .background(handleColor, RoundedCornerShape(2.dp))
            )

            // Corner handle (visual cue for both-axes resize zone)
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(CORNER_SIZE)
                    .background(handleColor, RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
private fun ZButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

/**
 * Absolute-positioning container for sheet boxes.
 *
 * Each child must be wrapped in [EditableSheetBox], which tags it with
 * `Modifier.layoutId(BoxId.NAME)`. For every box:
 *   - if [positions] has an entry, it's placed at that (x, y) and sized to
 *     the stored width/height (0 on either axis = use the default size for
 *     that axis);
 *   - otherwise it's placed by the default row flow ([rows]), sharing its
 *     row's width evenly and wrapping its own height.
 *
 * Computing the full default layout first and then overriding per stored
 * position keeps default slots stable: moving one box never reflows the
 * others, which is what you want for a free-form sheet.
 *
 * The canvas reports a height equal to the lowest box's bottom edge plus
 * [bottomPadding], with a floor of the incoming min height, so it scrolls
 * naturally inside a parent `verticalScroll`.
 */
@Composable
fun SheetCanvas(
    positions: Map<String, BoxPosition>,
    rows: List<List<BoxId>>,
    modifier: Modifier = Modifier,
    gap: Dp = 12.dp,
    bottomPadding: Dp = 24.dp,
    content: @Composable () -> Unit
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val gapPx    = gap.roundToPx()
        val bottomPx = bottomPadding.roundToPx()
        val maxW     = constraints.maxWidth

        val byId: Map<String, androidx.compose.ui.layout.Measurable> =
            measurables.associateBy { (it.layoutId as? BoxId)?.name ?: (it.layoutId?.toString() ?: "") }

        // --- default width + x within each row ----------------------------
        val defaultWidth = HashMap<String, Int>()
        val defaultX     = HashMap<String, Int>()
        for (row in rows) {
            val present = row.map { it.name }.filter { byId.containsKey(it) }
            if (present.isEmpty()) continue
            val n     = present.size
            val share = ((maxW - (n - 1) * gapPx) / n).coerceAtLeast(1)
            var x = 0
            present.forEach { id ->
                defaultWidth[id] = share
                defaultX[id]     = x
                x += share + gapPx
            }
        }

        // --- measure ------------------------------------------------------
        data class Measured(
            val id: String,
            val placeable: androidx.compose.ui.layout.Placeable,
            val pos: BoxPosition?
        )

        val measured = byId.entries.map { (id, measurable) ->
            val pos        = positions[id]
            val effWidthPx = when {
                pos != null && pos.width > 0f -> pos.width.dp.roundToPx().coerceAtMost(maxW)
                else                          -> defaultWidth[id] ?: maxW
            }
            val childConstraints = if (pos != null && pos.height > 0f) {
                Constraints.fixed(effWidthPx, pos.height.dp.roundToPx())
            } else {
                Constraints(minWidth = effWidthPx, maxWidth = effWidthPx,
                            minHeight = 0, maxHeight = Constraints.Infinity)
            }
            Measured(id, measurable.measure(childConstraints), pos)
        }

        // --- default y by flowing rows using measured heights -------------
        val defaultY   = HashMap<String, Int>()
        val heightById = measured.associate { it.id to it.placeable.height }
        var rowY = 0
        for (row in rows) {
            val present = row.map { it.name }.filter { byId.containsKey(it) }
            if (present.isEmpty()) continue
            present.forEach { id -> defaultY[id] = rowY }
            val rowHeight = present.maxOf { heightById[it] ?: 0 }
            rowY += rowHeight + gapPx
        }

        // --- final placement ----------------------------------------------
        fun placeX(m: Measured): Int =
            if (m.pos != null) m.pos.x.dp.roundToPx() else (defaultX[m.id] ?: 0)
        fun placeY(m: Measured): Int =
            if (m.pos != null) m.pos.y.dp.roundToPx() else (defaultY[m.id] ?: 0)

        val maxBottom   = measured.maxOfOrNull { placeY(it) + it.placeable.height } ?: 0
        val totalHeight = (maxBottom + bottomPx).coerceAtLeast(constraints.minHeight)

        layout(maxW, totalHeight) {
            // Draw order: ascending z. Stable sort preserves measure order for ties.
            measured.sortedBy { it.pos?.z ?: 0 }.forEach { m ->
                m.placeable.place(x = placeX(m), y = placeY(m))
            }
        }
    }
}
