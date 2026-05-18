package com.dndsheet.app.ui.character.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * A small filled dot rendered next to a calculated value to flag that the
 * user has manually pinned it. Per spec §8 the indicator is always shown
 * (not just on hover) so the user can spot pins at a glance.
 *
 * Pass `visible = false` to render an invisible spacer of the same size,
 * so rows with and without overrides line up vertically.
 */
@Composable
fun PinnedIndicator(visible: Boolean, modifier: Modifier = Modifier) {
    val color = if (visible) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.primary.copy(alpha = 0f)
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}
