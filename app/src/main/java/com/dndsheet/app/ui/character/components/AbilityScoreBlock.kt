package com.dndsheet.app.ui.character.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dndsheet.app.ui.character.formatModifier

/**
 * The classic D&D ability box: large modifier on top, smaller raw score below,
 * label across the top. The pinned dot sits in the top-right corner when the
 * modifier has been overridden.
 *
 * @param abilityMod the +/- ability modifier, NOT the Compose Modifier.
 * @param modifier   standard Compose modifier slot.
 */
@Composable
fun AbilityScoreBlock(
    label: String,
    score: Int,
    abilityMod: Int,
    isOverridden: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .then(clickModifier)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = formatModifier(abilityMod),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = score.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        PinnedIndicator(
            visible = isOverridden,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
        )
    }
}
