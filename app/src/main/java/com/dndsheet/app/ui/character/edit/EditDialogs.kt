package com.dndsheet.app.ui.character.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dndsheet.domain.enums.Alignment as AlignmentEnum

/**
 * Free-form text field. [validate] runs on every keystroke; the confirm
 * button is disabled while it returns false.
 */
@Composable
fun TextFieldDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    label: String = title,
    validate: (String) -> Boolean = { true }
) {
    var text by remember { mutableStateOf(initial) }
    val ok = validate(text)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text); onDismiss() }, enabled = ok) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Single-int field. Clamps to [range] in real time so the confirm value
 * is always valid.
 */
@Composable
fun NumberFieldDialog(
    title: String,
    initial: Int,
    range: IntRange,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    label: String = title
) {
    var raw by remember { mutableStateOf(initial.toString()) }
    val parsed = raw.toIntOrNull()
    val ok = parsed != null && parsed in range

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = raw,
                    onValueChange = { raw = it.filter { ch -> ch.isDigit() || ch == '-' } },
                    label = { Text(label) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Allowed: ${range.first}–${range.last}",
                    modifier = Modifier.padding(top = 6.dp),
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(parsed!!); onDismiss() }, enabled = ok) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Quick HP adjustment dialog: enter a positive number, the caller decides
 * whether to add (heal) or subtract (damage). Available outside edit mode
 * so combat HP changes don't require toggling the sheet into edit state.
 */
@Composable
fun HpAdjustDialog(
    isHeal: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (amount: Int) -> Unit
) {
    var raw by remember { mutableStateOf("") }
    val amount = raw.toIntOrNull()
    val ok = amount != null && amount > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isHeal) "Heal" else "Damage taken") },
        text = {
            OutlinedTextField(
                value = raw,
                onValueChange = { raw = it.filter(Char::isDigit).take(4) },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(amount!!); onDismiss() },
                enabled = ok
            ) { Text(if (isHeal) "Heal" else "Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Two-int dialog for HP (current / max). Temp HP is intentionally absent —
 * the spec calls for handling it via freehand annotation over the sheet
 * rather than as a structured field. The [com.dndsheet.domain.model.Character]
 * still carries a `temporaryHp` field for forward compatibility, but no
 * surface in the UI touches it.
 */
@Composable
fun HpDialog(
    initialCurrent: Int,
    initialMax: Int,
    onDismiss: () -> Unit,
    onConfirm: (current: Int, max: Int) -> Unit
) {
    var max by remember { mutableStateOf(initialMax.toString()) }
    var current by remember { mutableStateOf(initialCurrent.toString()) }

    val maxN = max.toIntOrNull()
    val currN = current.toIntOrNull()
    val ok = maxN != null && currN != null &&
        maxN in 0..999 && currN in 0..999

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hit Points") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = current,
                    onValueChange = { current = it.filter(Char::isDigit) },
                    label = { Text("Current") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = max,
                    onValueChange = { max = it.filter(Char::isDigit) },
                    label = { Text("Max") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(currN!!, maxN!!)
                    onDismiss()
                },
                enabled = ok
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * The classic D&D alignment matrix: Lawful / Neutral / Chaotic across the
 * top, Good / Neutral / Evil down the side. "Unaligned" lives below the
 * grid for creatures (and players) that opt out of the chart entirely.
 *
 * Chips use the two-letter abbreviations to keep the grid square at a
 * reasonable dialog width; the full name of the current selection shows
 * above the grid so the abbreviation isn't load-bearing for readability.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlignmentDialog(
    initial: AlignmentEnum,
    onDismiss: () -> Unit,
    onConfirm: (AlignmentEnum) -> Unit
) {
    var selected by remember { mutableStateOf(initial) }

    // 3×3 layout: rows are ethical (good/neutral/evil), columns are moral
    // (lawful/neutral/chaotic).
    val grid: List<List<AlignmentEnum>> = listOf(
        listOf(AlignmentEnum.LAWFUL_GOOD,    AlignmentEnum.NEUTRAL_GOOD,    AlignmentEnum.CHAOTIC_GOOD),
        listOf(AlignmentEnum.LAWFUL_NEUTRAL, AlignmentEnum.TRUE_NEUTRAL,    AlignmentEnum.CHAOTIC_NEUTRAL),
        listOf(AlignmentEnum.LAWFUL_EVIL,    AlignmentEnum.NEUTRAL_EVIL,    AlignmentEnum.CHAOTIC_EVIL)
    )
    val rowLabels = listOf("Good", "Neutral", "Evil")
    val columnLabels = listOf("Lawful", "Neutral", "Chaotic")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alignment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = selected.display,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                // Column headers — empty corner cell, then three labels.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.width(60.dp))
                    columnLabels.forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                // 3×3 chip grid with row labels down the left.
                grid.forEachIndexed { rowIndex, row ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = rowLabels[rowIndex],
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.width(60.dp)
                        )
                        row.forEach { alignment ->
                            FilterChip(
                                selected = selected == alignment,
                                onClick = { selected = alignment },
                                label = {
                                    Text(
                                        text = abbreviate(alignment),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 2.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Unaligned sits below the matrix — outside the moral/ethical grid.
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    Box(modifier = Modifier.width(60.dp))
                    FilterChip(
                        selected = selected == AlignmentEnum.UNALIGNED,
                        onClick = { selected = AlignmentEnum.UNALIGNED },
                        label = { Text("Unaligned") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected); onDismiss() }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun abbreviate(alignment: AlignmentEnum): String = when (alignment) {
    AlignmentEnum.LAWFUL_GOOD     -> "LG"
    AlignmentEnum.NEUTRAL_GOOD    -> "NG"
    AlignmentEnum.CHAOTIC_GOOD    -> "CG"
    AlignmentEnum.LAWFUL_NEUTRAL  -> "LN"
    AlignmentEnum.TRUE_NEUTRAL    -> "TN"
    AlignmentEnum.CHAOTIC_NEUTRAL -> "CN"
    AlignmentEnum.LAWFUL_EVIL     -> "LE"
    AlignmentEnum.NEUTRAL_EVIL    -> "NE"
    AlignmentEnum.CHAOTIC_EVIL    -> "CE"
    AlignmentEnum.UNALIGNED       -> "—"
}

/**
 * Single-select picker for an enum. Renders options as a wrapping row of
 * filter chips — works well for short option lists like rulesets. For
 * alignments, prefer [AlignmentDialog] which lays them out in the classic
 * 3×3 matrix.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun <T> SelectDialog(
    title: String,
    options: List<T>,
    initial: T,
    label: (T) -> String,
    onDismiss: () -> Unit,
    onConfirm: (T) -> Unit
) {
    var selected by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                options.forEach { option ->
                    FilterChip(
                        selected = option == selected,
                        onClick = { selected = option },
                        label = { Text(label(option)) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected); onDismiss() }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
