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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
    CONDITIONS,

    // ── Spellcasting (5.5e / 2024) — one box per spellcasting class, up to 6 ──
    // Slots are filled in order; unused slots produce no child, so the canvas
    // row collapses automatically when fewer than N classes are spellcasters.
    SPELLCAST_CLASS_1, SPELLCAST_CLASS_2, SPELLCAST_CLASS_3,
    SPELLCAST_CLASS_4, SPELLCAST_CLASS_5, SPELLCAST_CLASS_6,

    // ── Spellcasting (5e / 2014) — three fixed summary boxes ──────────────────
    SPELLCAST_ABILITY, SPELLCAST_DC, SPELLCAST_ATTACK
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
        // 5e spellcasting — three summary boxes side-by-side.
        add(listOf(BoxId.SPELLCAST_ABILITY, BoxId.SPELLCAST_DC, BoxId.SPELLCAST_ATTACK))
    } else {
        add(listOf(BoxId.SAVE_STR, BoxId.SAVE_DEX, BoxId.SAVE_CON))
        add(listOf(BoxId.SAVE_INT, BoxId.SAVE_WIS, BoxId.SAVE_CHA))
        // In 2024 mode skills are rendered inside the SAVE_* boxes —
        // no separate SKILLS_* rows in the default flow.
        // 5.5e spellcasting — one box per class, two per row (up to 6 classes).
        add(listOf(BoxId.SPELLCAST_CLASS_1, BoxId.SPELLCAST_CLASS_2))
        add(listOf(BoxId.SPELLCAST_CLASS_3, BoxId.SPELLCAST_CLASS_4))
        add(listOf(BoxId.SPELLCAST_CLASS_5, BoxId.SPELLCAST_CLASS_6))
    }
    add(listOf(BoxId.PASSIVES))
    add(listOf(BoxId.CONDITIONS))
}

/**
 * Carries the per-box font-scale factor set by [EditableSheetBox] to any
 * descendant composable that needs to size non-text elements (dots, padding,
 * icon sizes) proportionally. Defaults to 1f when read outside a box.
 */
val LocalBoxFontScale = compositionLocalOf { 1f }

/**
 * Scales all 15 Material3 text styles by [factor] so content wrapped in
 * `MaterialTheme(typography = baseTypography.scaled(factor))` gets a uniform
 * font-size bump/shrink without touching individual composables.
 */
private fun Typography.scaled(factor: Float): Typography = Typography(
    displayLarge   = displayLarge.copy(fontSize   = displayLarge.fontSize   * factor),
    displayMedium  = displayMedium.copy(fontSize  = displayMedium.fontSize  * factor),
    displaySmall   = displaySmall.copy(fontSize   = displaySmall.fontSize   * factor),
    headlineLarge  = headlineLarge.copy(fontSize  = headlineLarge.fontSize  * factor),
    headlineMedium = headlineMedium.copy(fontSize = headlineMedium.fontSize * factor),
    headlineSmall  = headlineSmall.copy(fontSize  = headlineSmall.fontSize  * factor),
    titleLarge  = titleLarge.copy(fontSize  = titleLarge.fontSize  * factor),
    titleMedium = titleMedium.copy(fontSize = titleMedium.fontSize * factor),
    titleSmall  = titleSmall.copy(fontSize  = titleSmall.fontSize  * factor),
    bodyLarge  = bodyLarge.copy(fontSize  = bodyLarge.fontSize  * factor),
    bodyMedium = bodyMedium.copy(fontSize = bodyMedium.fontSize * factor),
    bodySmall  = bodySmall.copy(fontSize  = bodySmall.fontSize  * factor),
    labelLarge  = labelLarge.copy(fontSize  = labelLarge.fontSize  * factor),
    labelMedium = labelMedium.copy(fontSize = labelMedium.fontSize * factor),
    labelSmall  = labelSmall.copy(fontSize  = labelSmall.fontSize  * factor)
)

/**
 * Wraps a sheet-box composable, tagging it with [boxId] for [SheetCanvas] to
 * find, and — in edit mode — enabling drag-to-move, edge/corner resize, z-order
 * step buttons, and per-box font-size +/− controls.
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
 * **Z-order buttons**: small ▲/▼ buttons at the top-end corner. Taps don't
 * conflict with the drag detector (no slop threshold is reached by a tap, so
 * [detectDragGestures] stays idle for taps).
 *
 * **Font-scale buttons**: small +/− buttons at the top-start corner, visible
 * only in edit mode. Each tap calls [onFontScale] with ±1; the caller applies
 * the actual 0.1-step and coerces the range. All box content is wrapped in
 * `MaterialTheme(typography = scaled)` so font size changes propagate
 * uniformly without touching individual composables.
 */
@Composable
fun EditableSheetBox(
    boxId: BoxId,
    editing: Boolean,
    onMove: (BoxId, xDp: Float, yDp: Float) -> Unit,
    onResize: (BoxId, widthDp: Float?, heightDp: Float?) -> Unit,
    onZChange: (BoxId, delta: Int) -> Unit,
    onFontScale: (BoxId, Int) -> Unit,
    fontScale: Float = 1f,
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
        // Scale all text inside the box by the stored fontScale factor.
        // LocalBoxFontScale also makes the scale available to composables that
        // size non-text elements (proficiency dots, row padding) proportionally.
        // remember(fontScale) avoids rebuilding the Typography object every frame.
        val baseTypography = MaterialTheme.typography
        val scaledTypography = remember(fontScale) { baseTypography.scaled(fontScale) }
        CompositionLocalProvider(LocalBoxFontScale provides fontScale) {
        MaterialTheme(typography = scaledTypography) {
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
        } // end MaterialTheme
        } // end CompositionLocalProvider

        if (editing) {
            val zColor  = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
            val zTint   = MaterialTheme.colorScheme.onPrimaryContainer
            val fsColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
            val fsTint  = MaterialTheme.colorScheme.onSecondaryContainer
            val handleColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)

            // Font-scale +/− buttons — top-start corner
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                EditButton(Icons.Default.Add,    fsColor, fsTint) { onFontScale(boxId, +1) }
                EditButton(Icons.Default.Remove, fsColor, fsTint) { onFontScale(boxId, -1) }
            }

            // Z-order step buttons — top-end corner
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                EditButton(Icons.Default.KeyboardArrowUp,   zColor, zTint) { onZChange(boxId, +1) }
                EditButton(Icons.Default.KeyboardArrowDown, zColor, zTint) { onZChange(boxId, -1) }
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

/** Small 20×20dp circular button used for both z-order and font-scale controls. */
@Composable
private fun EditButton(
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = contentColor
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
