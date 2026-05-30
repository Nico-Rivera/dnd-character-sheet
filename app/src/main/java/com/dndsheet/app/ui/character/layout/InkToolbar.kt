package com.dndsheet.app.ui.character.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixOff
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Fixed toolbar shown only while ink mode is active. Contains:
 *
 * - **Tool buttons**: Pen (active), Eraser (disabled until commit 11),
 *   Selection (disabled until commit 12).
 * - **Color palette**: eight preset ink colors as tappable chips.
 * - **Width slider**: 1 dp – 20 dp, label shows the current integer value.
 * - **Undo / Redo**: enabled by the caller when the respective stack is non-empty.
 *
 * The toolbar renders at its natural height and sits *outside* the
 * vertically-scrollable column so it stays pinned to the top while the
 * sheet scrolls.
 */
@Composable
fun InkToolbar(
    activeTool: InkTool,
    onToolChange: (InkTool) -> Unit,
    penColor: Long,
    onColorChange: (Long) -> Unit,
    penWidth: Float,
    onWidthChange: (Float) -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 4.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // ── Tool buttons ──────────────────────────────────────────────
            InkToolButton(
                icon     = Icons.Default.Draw,
                label    = "Pen",
                selected = activeTool == InkTool.PEN,
                enabled  = true,
                onClick  = { onToolChange(InkTool.PEN) }
            )
            InkToolButton(
                icon     = Icons.Default.AutoFixOff,
                label    = "Eraser",
                selected = activeTool == InkTool.ERASER,
                enabled  = true,
                onClick  = { onToolChange(InkTool.ERASER) }
            )
            InkToolButton(
                icon     = Icons.Default.CropFree,
                label    = "Select",
                selected = activeTool == InkTool.SELECTION,
                enabled  = true,
                onClick  = { onToolChange(InkTool.SELECTION) }
            )

            VerticalDivider(modifier = Modifier.height(28.dp).padding(horizontal = 2.dp))

            // ── Color palette ─────────────────────────────────────────────
            INK_PALETTE.forEach { colorInt ->
                val colorLong = colorInt.toLong()
                InkColorChip(
                    color    = Color(colorInt),
                    selected = penColor == colorLong,
                    onClick  = { onColorChange(colorLong) }
                )
            }

            VerticalDivider(modifier = Modifier.height(28.dp).padding(horizontal = 2.dp))

            // ── Width slider ──────────────────────────────────────────────
            Text(
                text  = penWidth.toInt().toString(),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.width(18.dp)
            )
            Slider(
                value         = penWidth,
                onValueChange = onWidthChange,
                valueRange    = 1f..20f,
                modifier      = Modifier.width(88.dp)
            )

            VerticalDivider(modifier = Modifier.height(28.dp).padding(horizontal = 2.dp))

            // ── Undo / Redo ───────────────────────────────────────────────
            IconButton(
                onClick  = onUndo,
                enabled  = canUndo,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Undo, contentDescription = "Undo")
            }
            IconButton(
                onClick  = onRedo,
                enabled  = canRedo,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Redo, contentDescription = "Redo")
            }
        }
    }
}

@Composable
private fun InkToolButton(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector         = icon,
            contentDescription  = label,
            tint                = if (enabled) MaterialTheme.colorScheme.onSurface
                                  else         MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            modifier            = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun InkColorChip(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (selected)
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                else
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
            .clickable(onClick = onClick)
    )
}
