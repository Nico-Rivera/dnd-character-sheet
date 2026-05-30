package com.dndsheet.app.ui.character.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import com.dndsheet.domain.model.ClassPreset
import com.dndsheet.domain.model.ClassPresets
import com.dndsheet.domain.model.SpellcastingProgression
import com.dndsheet.domain.model.SubclassPreset

/**
 * Dialog for adding a new class entry. Provides two modes that blend together:
 *
 * **Preset mode** — tapping a known class chip auto-fills all fields. If the
 * class has known subclasses, a second chip row appears. Selecting a subclass
 * that grants spellcasting (Eldritch Knight, Arcane Trickster) further
 * overrides the progression and spellcasting ability so the user never has to
 * configure these fields manually.
 *
 * **Custom mode** — the user can type any class name and manually set hit die,
 * progression, and ability. This mode is used when the preset chips aren't
 * shown or when the user clears the chip selection.
 *
 * Both modes produce a valid [ClassLevel] if the confirm button is enabled.
 * Validation rules are identical to before:
 *  - className not blank
 *  - hitDie ∈ {6, 8, 10, 12}
 *  - if progression != NONE, an ability must be set
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddClassDialog(
    onDismiss: () -> Unit,
    onConfirm: (ClassLevel) -> Unit
) {
    // ── Preset selection state ────────────────────────────────────────────────
    var selectedPreset    by remember { mutableStateOf<ClassPreset?>(null) }
    var selectedSubclass  by remember { mutableStateOf<SubclassPreset?>(null) }

    // ── Manual / derived field state ──────────────────────────────────────────
    var className   by remember { mutableStateOf("") }
    var subclassStr by remember { mutableStateOf("") }
    var hitDie      by remember { mutableStateOf(8) }
    var progression by remember { mutableStateOf(SpellcastingProgression.NONE) }
    var ability     by remember { mutableStateOf<Ability?>(null) }

    // Apply a class preset, wiping any existing subclass selection.
    fun applyPreset(preset: ClassPreset) {
        selectedPreset   = preset
        selectedSubclass = null
        className        = preset.className
        subclassStr      = ""
        hitDie           = preset.hitDie
        progression      = preset.progression
        ability          = preset.spellcastingAbility
    }

    // Apply a subclass preset on top of the current class preset.
    fun applySubclass(sub: SubclassPreset) {
        selectedSubclass = sub
        subclassStr      = sub.subclassName
        val cp = selectedPreset ?: return
        progression      = ClassPresets.effectiveProgression(cp, sub)
        ability          = ClassPresets.effectiveAbility(cp, sub)
    }

    // Deselect a subclass, restoring the parent class's values.
    fun clearSubclass() {
        selectedSubclass = null
        subclassStr      = ""
        val cp = selectedPreset ?: return
        progression      = cp.progression
        ability          = cp.spellcastingAbility
    }

    val needsAbility = progression != SpellcastingProgression.NONE
    val ok = className.isNotBlank() && hitDie in listOf(6, 8, 10, 12) &&
        (!needsAbility || ability != null)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add class") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                // ── Known class chips ─────────────────────────────────────────
                Text("Quick-select")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ClassPresets.all.forEach { preset ->
                        FilterChip(
                            selected = selectedPreset?.className == preset.className,
                            onClick  = {
                                if (selectedPreset?.className == preset.className) {
                                    // Deselect — clear everything
                                    selectedPreset   = null
                                    selectedSubclass = null
                                    className        = ""
                                    subclassStr      = ""
                                } else {
                                    applyPreset(preset)
                                }
                            },
                            label = { Text(preset.className) }
                        )
                    }
                }

                // ── Known subclass chips (only when class has some) ───────────
                val knownSubclasses = selectedPreset?.subclasses.orEmpty()
                if (knownSubclasses.isNotEmpty()) {
                    Text("Subclass")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        knownSubclasses.forEach { sub ->
                            FilterChip(
                                selected = selectedSubclass?.subclassName == sub.subclassName,
                                onClick  = {
                                    if (selectedSubclass?.subclassName == sub.subclassName) {
                                        clearSubclass()
                                    } else {
                                        applySubclass(sub)
                                    }
                                },
                                label = { Text(sub.subclassName) }
                            )
                        }
                    }
                }

                HorizontalDivider()

                // ── Manual fields (always visible; pre-filled by chips) ────────
                OutlinedTextField(
                    value = className,
                    onValueChange = { className = it; selectedPreset = null },
                    label = { Text("Class") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subclassStr,
                    onValueChange = {
                        subclassStr = it
                        // If the user edits the field, detach from the chip selection.
                        if (selectedSubclass != null && it != selectedSubclass?.subclassName) {
                            selectedSubclass = null
                        }
                    },
                    label = { Text("Subclass (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Hit die")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(6, 8, 10, 12).forEach { d ->
                        FilterChip(
                            selected = hitDie == d,
                            onClick  = { hitDie = d },
                            label    = { Text("d$d") }
                        )
                    }
                }

                Text("Spellcasting")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SpellcastingProgression.entries.forEach { p ->
                        FilterChip(
                            selected = progression == p,
                            onClick  = {
                                progression = p
                                if (p == SpellcastingProgression.NONE) ability = null
                                // Detach from chip if user manually changes progression.
                                selectedSubclass = null
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
                                onClick  = { ability = a },
                                label    = { Text(a.abbr) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        ClassLevel(
                            className           = className.trim(),
                            level               = 1,
                            subclass            = subclassStr.ifBlank { null }?.trim(),
                            hitDie              = hitDie,
                            progression         = progression,
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
