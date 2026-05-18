package com.dndsheet.app.ui.character.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dndsheet.domain.enums.Ability
import com.dndsheet.domain.model.ClassLevel
import com.dndsheet.domain.model.SpellcastingProgression

/**
 * Dialog for adding a new class entry to a character. Captures everything
 * the [ClassLevel] init block requires so the data class doesn't throw
 * when we hand the values back. Validation rules:
 *
 *   - className not blank
 *   - hitDie ∈ {6, 8, 10, 12}
 *   - if progression != NONE, an ability must be picked
 *
 * The level field starts at 1 (the implicit "joined this class" baseline);
 * adjusting level up/down happens on the row in edit mode, not here.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddClassDialog(
    onDismiss: () -> Unit,
    onConfirm: (ClassLevel) -> Unit
) {
    var className by remember { mutableStateOf("") }
    var subclass by remember { mutableStateOf("") }
    var hitDie by remember { mutableStateOf(8) }
    var progression by remember { mutableStateOf(SpellcastingProgression.NONE) }
    var ability by remember { mutableStateOf<Ability?>(null) }

    val needsAbility = progression != SpellcastingProgression.NONE
    val ok = className.isNotBlank() && hitDie in listOf(6, 8, 10, 12) &&
        (!needsAbility || ability != null)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add class") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = className,
                    onValueChange = { className = it },
                    label = { Text("Class") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subclass,
                    onValueChange = { subclass = it },
                    label = { Text("Subclass (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Hit die")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(6, 8, 10, 12).forEach { d ->
                        FilterChip(
                            selected = hitDie == d,
                            onClick = { hitDie = d },
                            label = { Text("d$d") }
                        )
                    }
                }

                Text("Spellcasting")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SpellcastingProgression.entries.forEach { p ->
                        FilterChip(
                            selected = progression == p,
                            onClick = {
                                progression = p
                                if (p == SpellcastingProgression.NONE) ability = null
                            },
                            label = { Text(p.name.lowercase().replaceFirstChar { it.titlecase() }) }
                        )
                    }
                }

                if (needsAbility) {
                    Text("Spellcasting ability")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Ability.entries.forEach { a ->
                            FilterChip(
                                selected = ability == a,
                                onClick = { ability = a },
                                label = { Text(a.abbr) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        ClassLevel(
                            className = className.trim(),
                            level = 1,
                            subclass = subclass.ifBlank { null }?.trim(),
                            hitDie = hitDie,
                            progression = progression,
                            spellcastingAbility = ability
                        )
                    )
                    onDismiss()
                },
                enabled = ok
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
