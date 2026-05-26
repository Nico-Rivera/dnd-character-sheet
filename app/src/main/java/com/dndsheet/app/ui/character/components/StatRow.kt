package com.dndsheet.app.ui.character.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dndsheet.app.ui.character.formatModifier
import com.dndsheet.app.ui.character.layout.LocalBoxFontScale
import com.dndsheet.domain.enums.ProficiencyLevel

/**
 * One row of "[prof tier dot]   label   bonus   [pinned dot]" — used for
 * skills and saves alike. Proficiency tier is encoded by the leading dot:
 *
 *   NONE       → empty outline
 *   HALF       → half-filled
 *   PROFICIENT → filled
 *   EXPERTISE  → filled with a thick secondary-color ring
 *
 * The dot size and row vertical padding both scale with [LocalBoxFontScale]
 * so that reducing the font size via the per-box +/− controls also tightens
 * the spacing and shrinks the non-text markers proportionally.
 */
@Composable
fun StatRow(
    label: String,
    bonus: Int,
    proficiencyTier: ProficiencyLevel,
    isOverridden: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val scale = LocalBoxFontScale.current
    val verticalPad = (3.dp * scale).coerceAtLeast(1.dp)

    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(clickModifier)
            .padding(horizontal = 4.dp, vertical = verticalPad),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProficiencyDot(proficiencyTier, scale)
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f)
        )
        Text(
            text = formatModifier(bonus),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        PinnedIndicator(
            visible = isOverridden,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun ProficiencyDot(tier: ProficiencyLevel, scale: Float) {
    val primary   = MaterialTheme.colorScheme.primary
    val outline   = MaterialTheme.colorScheme.outline
    val secondary = MaterialTheme.colorScheme.secondary

    // Per-tier visual: (fill, ring color, base ring width).
    // EXPERTISE is rendered larger with a thicker contrasting ring so it
    // reads as "more than proficient" at a glance.
    val baseSize = if (tier == ProficiencyLevel.EXPERTISE) 16.dp else 12.dp
    val size = baseSize * scale

    val (fill, ringColor, baseRingWidth) = when (tier) {
        ProficiencyLevel.NONE       -> Triple(Color.Transparent, outline, 1.dp)
        ProficiencyLevel.HALF       -> Triple(primary.copy(alpha = 0.45f), outline, 1.dp)
        ProficiencyLevel.PROFICIENT -> Triple(primary, primary, 0.dp)
        ProficiencyLevel.EXPERTISE  -> Triple(primary, secondary, 3.dp)
    }
    val ringWidth = baseRingWidth * scale

    // Reserve a fixed slot (scaled) regardless of dot size so labels in
    // different rows still line up vertically.
    val slotSize = 16.dp * scale
    Box(
        modifier = Modifier.size(slotSize),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .background(color = fill, shape = CircleShape)
                .border(width = ringWidth, color = ringColor, shape = CircleShape)
        )
    }
}
