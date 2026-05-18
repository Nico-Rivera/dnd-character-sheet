package com.dndsheet.app.ui.character

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dndsheet.app.DnDApplication
import com.dndsheet.app.ui.character.components.AbilityScoreBlock
import com.dndsheet.app.ui.character.components.PinnedIndicator
import com.dndsheet.app.ui.character.components.StatRow
import com.dndsheet.app.ui.character.edit.ActiveDialog
import com.dndsheet.app.ui.character.edit.AddClassDialog
import com.dndsheet.app.ui.character.edit.AlignmentDialog
import com.dndsheet.app.ui.character.edit.HpAdjustDialog
import com.dndsheet.app.ui.character.edit.HpDialog
import com.dndsheet.app.ui.character.edit.NumberFieldDialog
import com.dndsheet.app.ui.character.edit.SelectDialog
import com.dndsheet.app.ui.character.edit.TextFieldDialog
import androidx.compose.material3.OutlinedButton
import com.dndsheet.domain.enums.Ability
import com.dndsheet.domain.enums.Alignment as AlignmentEnum
import com.dndsheet.domain.enums.ProficiencyLevel
import com.dndsheet.domain.enums.Ruleset
import com.dndsheet.domain.enums.Skill
import com.dndsheet.domain.model.AbilityScores
import com.dndsheet.domain.model.Character
import com.dndsheet.domain.model.ClassLevel
import com.dndsheet.rules.AbilityCalculator
import com.dndsheet.rules.PassiveCalculator
import com.dndsheet.rules.ProficiencyCalculator
import com.dndsheet.rules.SavingThrowCalculator
import com.dndsheet.rules.SkillCalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterOverviewScreen(
    characterId: String,
    onBack: () -> Unit,
    viewModel: CharacterOverviewViewModel = vmFromContainer(characterId)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Edit mode and the currently-open dialog are screen-local state — they
    // don't survive process death, which is fine for transient edit UI.
    var editing by remember { mutableStateOf(false) }
    var activeDialog by remember { mutableStateOf<ActiveDialog>(ActiveDialog.None) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = if (editing) TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) else TopAppBarDefaults.topAppBarColors(),
                title = {
                    Text(
                        when (val s = state) {
                            is OverviewState.Loaded -> s.character.name.ifBlank { "Unnamed" }
                            else -> "Character"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { editing = !editing; activeDialog = ActiveDialog.None }) {
                        if (editing) Icon(Icons.Default.Check, contentDescription = "Done editing")
                        else Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val s = state) {
                OverviewState.Loading -> CenteredText("Loading…")
                OverviewState.NotFound -> CenteredText("Character not found.")
                is OverviewState.Loaded -> SheetBody(
                    character = s.character,
                    editing = editing,
                    onOpenDialog = { activeDialog = it },
                    onCycleSkillProf = { skill ->
                        viewModel.update { it.cycleSkillProf(skill) }
                    },
                    onCycleSaveProf = { ability ->
                        viewModel.update { it.cycleSaveProf(ability) }
                    },
                    onLevel = { className, delta ->
                        viewModel.update { it.adjustLevel(className, delta) }
                    },
                    onRemoveClass = { className ->
                        viewModel.update { it.removeClass(className) }
                    }
                )
            }

            // Dialogs pull the current character from `state` rather than the
            // dialog enum so they always show fresh values when reopened.
            (state as? OverviewState.Loaded)?.let { loaded ->
                EditDialogHost(
                    character = loaded.character,
                    active = activeDialog,
                    onDismiss = { activeDialog = ActiveDialog.None },
                    onApply = { transform -> viewModel.update(transform) }
                )
            }
        }
    }
}

@Composable
private fun EditDialogHost(
    character: Character,
    active: ActiveDialog,
    onDismiss: () -> Unit,
    onApply: ((Character) -> Character) -> Unit
) {
    when (active) {
        ActiveDialog.None -> Unit

        ActiveDialog.EditName -> TextFieldDialog(
            title = "Name", initial = character.name,
            validate = { it.isNotBlank() },
            onDismiss = onDismiss,
            onConfirm = { v -> onApply { it.copy(name = v.trim()) } }
        )
        ActiveDialog.EditSpecies -> TextFieldDialog(
            title = "Species / Race", initial = character.species,
            onDismiss = onDismiss,
            onConfirm = { v -> onApply { it.copy(species = v.trim()) } }
        )
        ActiveDialog.EditBackground -> TextFieldDialog(
            title = "Background", initial = character.background,
            onDismiss = onDismiss,
            onConfirm = { v -> onApply { it.copy(background = v.trim()) } }
        )
        ActiveDialog.EditAlignment -> AlignmentDialog(
            initial = character.alignment,
            onDismiss = onDismiss,
            onConfirm = { v -> onApply { it.copy(alignment = v) } }
        )
        ActiveDialog.EditRuleset -> SelectDialog(
            title = "Ruleset",
            options = Ruleset.entries,
            initial = character.ruleset,
            label = { it.display },
            onDismiss = onDismiss,
            onConfirm = { v -> onApply { it.copy(ruleset = v) } }
        )

        ActiveDialog.EditHp -> HpDialog(
            initialCurrent = character.currentHp,
            initialMax = character.maxHp,
            onDismiss = onDismiss,
            onConfirm = { current, max ->
                onApply {
                    // Clamp current to max on confirm so a lowered max doesn't
                    // leave currentHp out of bounds.
                    it.copy(
                        currentHp = current.coerceIn(0, max.coerceAtLeast(0)),
                        maxHp = max.coerceAtLeast(0)
                    )
                }
            }
        )

        is ActiveDialog.EditAbility -> NumberFieldDialog(
            title = active.ability.abbr,
            initial = character.abilityScores[active.ability],
            range = 1..30,
            onDismiss = onDismiss,
            onConfirm = { v ->
                onApply { it.copy(abilityScores = it.abilityScores.set(active.ability, v)) }
            }
        )

        ActiveDialog.AddClass -> AddClassDialog(
            onDismiss = onDismiss,
            onConfirm = { newClass ->
                onApply { it.copy(classes = it.classes + newClass) }
            }
        )

        is ActiveDialog.AdjustHp -> HpAdjustDialog(
            isHeal = active.isHeal,
            onDismiss = onDismiss,
            onConfirm = { amount ->
                onApply {
                    val signed = if (active.isHeal) amount else -amount
                    // Clamp on both ends so a big heal can't overshoot max
                    // and a big hit can't go below 0.
                    it.copy(
                        currentHp = (it.currentHp + signed).coerceIn(0, it.maxHp.coerceAtLeast(0))
                    )
                }
            }
        )
    }
}

@Composable
private fun CenteredText(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text)
    }
}

@Composable
private fun SheetBody(
    character: Character,
    editing: Boolean,
    onOpenDialog: (ActiveDialog) -> Unit,
    onCycleSkillProf: (Skill) -> Unit,
    onCycleSaveProf: (Ability) -> Unit,
    onLevel: (className: String, delta: Int) -> Unit,
    onRemoveClass: (className: String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HeaderBlock(
            character = character,
            editing = editing,
            onOpenDialog = onOpenDialog
        )
        if (editing) {
            ClassEditorSection(
                classes = character.classes,
                onAdd = { onOpenDialog(ActiveDialog.AddClass) },
                onLevel = onLevel,
                onRemove = onRemoveClass
            )
        }
        VitalsRow(character, editing, onOpenDialog)
        SectionHeader("Abilities")
        AbilitiesGrid(character, editing, onOpenDialog)
        SectionHeader("Saving Throws")
        SavesSection(character, editing, onCycleSaveProf)
        SectionHeader("Skills")
        SkillsSection(character, editing, onCycleSkillProf)
        SectionHeader("Passives")
        PassivesSection(character)
        if (character.conditions.isNotEmpty()) {
            SectionHeader("Conditions")
            Text(
                text = character.conditions.joinToString(", "),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HeaderBlock(
    character: Character,
    editing: Boolean,
    onOpenDialog: (ActiveDialog) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (editing) {
            // Show name as a tappable line — the name in the title bar is
            // bound to the same field so it updates everywhere on edit.
            EditableLine(
                label = "Name",
                value = character.name.ifBlank { "(unnamed)" },
                onClick = { onOpenDialog(ActiveDialog.EditName) }
            )
            EditableLine(
                label = "Species",
                value = character.species.ifBlank { "(none)" },
                onClick = { onOpenDialog(ActiveDialog.EditSpecies) }
            )
            EditableLine(
                label = "Background",
                value = character.background.ifBlank { "(none)" },
                onClick = { onOpenDialog(ActiveDialog.EditBackground) }
            )
            EditableLine(
                label = "Alignment",
                value = character.alignment.display,
                onClick = { onOpenDialog(ActiveDialog.EditAlignment) }
            )
            EditableLine(
                label = "Ruleset",
                value = character.ruleset.display,
                onClick = { onOpenDialog(ActiveDialog.EditRuleset) }
            )
        } else {
            Text(
                text = classLine(character),
                style = MaterialTheme.typography.titleMedium
            )
            val sub = listOfNotNull(
                character.species.takeIf { it.isNotBlank() },
                character.background.takeIf { it.isNotBlank() },
                character.alignment.display.takeIf { character.alignment != AlignmentEnum.UNALIGNED }
            ).joinToString(" • ")
            if (sub.isNotBlank()) {
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Text(
                text = character.ruleset.display,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun EditableLine(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Default.Edit,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ClassEditorSection(
    classes: List<ClassLevel>,
    onAdd: () -> Unit,
    onLevel: (className: String, delta: Int) -> Unit,
    onRemove: (className: String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionHeader("Classes")
        if (classes.isEmpty()) {
            Text(
                text = "No classes yet — add one to start leveling up.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        classes.forEach { cl ->
            ClassEditorRow(cl, onLevel = onLevel, onRemove = onRemove)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onAdd)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text("Add class", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ClassEditorRow(
    cl: ClassLevel,
    onLevel: (className: String, delta: Int) -> Unit,
    onRemove: (className: String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (cl.subclass != null) "${cl.className} (${cl.subclass})"
                else cl.className,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Level ${cl.level} • d${cl.hitDie}" +
                    (cl.spellcastingAbility?.let { " • ${it.abbr}" } ?: ""),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        IconButton(onClick = { onLevel(cl.className, -1) }, enabled = cl.level > 1) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease level")
        }
        IconButton(onClick = { onLevel(cl.className, +1) }, enabled = cl.level < 20) {
            Icon(Icons.Default.Add, contentDescription = "Increase level")
        }
        IconButton(onClick = { onRemove(cl.className) }) {
            Icon(Icons.Default.Delete, contentDescription = "Remove class")
        }
    }
}

@Composable
private fun VitalsRow(
    character: Character,
    editing: Boolean,
    onOpenDialog: (ActiveDialog) -> Unit
) {
    val pb = ProficiencyCalculator.bonus(character)
    val pbOver = ProficiencyCalculator.isOverridden(character)
    val initiative = PassiveCalculator.initiative(character)
    val initOver = character.overrides.initiative != null

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            VitalChip("Level", character.totalLevel.toString(), false, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            VitalChip("Prof", formatModifier(pb), pbOver, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            VitalChip("Init", formatModifier(initiative), initOver, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            VitalChip(
                label = "HP",
                value = "${character.currentHp}/${character.maxHp}",
                isOverridden = false,
                modifier = Modifier.weight(1f),
                onClick = if (editing) ({ onOpenDialog(ActiveDialog.EditHp) }) else null
            )
        }
        // Heal / damage row, aligned under the HP chip via a weight-3 spacer
        // so the two buttons share the same horizontal slot as the HP chip
        // above. Always tappable — these aren't gated by edit mode because
        // adjusting HP mid-encounter is the most common action on the sheet.
        Row(modifier = Modifier.fillMaxWidth()) {
            // 3 chips wide + 3 inter-chip spacers worth of empty space.
            Spacer(modifier = Modifier.weight(3f))
            Spacer(modifier = Modifier.width(24.dp))
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedButton(
                    onClick = { onOpenDialog(ActiveDialog.AdjustHp(isHeal = false)) },
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                ) { Text("−", style = MaterialTheme.typography.titleMedium) }
                OutlinedButton(
                    onClick = { onOpenDialog(ActiveDialog.AdjustHp(isHeal = true)) },
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                ) { Text("+", style = MaterialTheme.typography.titleMedium) }
            }
        }
    }
}

@Composable
private fun VitalChip(
    label: String,
    value: String,
    isOverridden: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val click = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
            .then(click)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        PinnedIndicator(
            visible = isOverridden,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
        )
    }
}

@Composable
private fun AbilitiesGrid(
    character: Character,
    editing: Boolean,
    onOpenDialog: (ActiveDialog) -> Unit
) {
    val rows = listOf(
        listOf(Ability.STR, Ability.DEX, Ability.CON),
        listOf(Ability.INT, Ability.WIS, Ability.CHA)
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (ability in row) {
                    AbilityScoreBlock(
                        label = ability.abbr,
                        score = character.abilityScores[ability],
                        abilityMod = AbilityCalculator.modifier(character, ability),
                        isOverridden = AbilityCalculator.isOverridden(character, ability),
                        onClick = if (editing) ({ onOpenDialog(ActiveDialog.EditAbility(ability)) }) else null
                    )
                }
            }
        }
    }
}

@Composable
private fun SavesSection(
    character: Character,
    editing: Boolean,
    onCycleSaveProf: (Ability) -> Unit
) {
    Column {
        for (ability in Ability.entries) {
            StatRow(
                label = ability.abbr,
                bonus = SavingThrowCalculator.bonus(character, ability),
                proficiencyTier = character.proficiencies[ability],
                isOverridden = SavingThrowCalculator.isOverridden(character, ability),
                onClick = if (editing) ({ onCycleSaveProf(ability) }) else null
            )
        }
    }
}

@Composable
private fun SkillsSection(
    character: Character,
    editing: Boolean,
    onCycleSkillProf: (Skill) -> Unit
) {
    Column {
        for (skill in Skill.entries) {
            StatRow(
                label = "${skill.display}  (${skill.ability.abbr})",
                bonus = SkillCalculator.bonus(character, skill),
                proficiencyTier = character.proficiencies[skill],
                isOverridden = SkillCalculator.isOverridden(character, skill),
                onClick = if (editing) ({ onCycleSkillProf(skill) }) else null
            )
        }
    }
}

@Composable
private fun PassivesSection(character: Character) {
    Column {
        StatRow(
            label = "Passive Perception",
            bonus = PassiveCalculator.perception(character),
            proficiencyTier = character.proficiencies[Skill.PERCEPTION],
            isOverridden = character.overrides.passivePerception != null
        )
        StatRow(
            label = "Passive Investigation",
            bonus = PassiveCalculator.investigation(character),
            proficiencyTier = character.proficiencies[Skill.INVESTIGATION],
            isOverridden = character.overrides.passiveInvestigation != null
        )
        StatRow(
            label = "Passive Insight",
            bonus = PassiveCalculator.insight(character),
            proficiencyTier = character.proficiencies[Skill.INSIGHT],
            isOverridden = character.overrides.passiveInsight != null
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Column {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 4.dp, bottom = 6.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    }
}

private fun classLine(character: Character): String {
    if (character.classes.isEmpty()) return "Classless"
    return character.classes.joinToString(" / ") { cl ->
        if (cl.subclass != null) "${cl.className} (${cl.subclass}) ${cl.level}"
        else "${cl.className} ${cl.level}"
    }
}

// ---- pure transforms used by the ViewModel update intents ----

private fun AbilityScores.set(ability: Ability, value: Int): AbilityScores = when (ability) {
    Ability.STR -> copy(strength = value)
    Ability.DEX -> copy(dexterity = value)
    Ability.CON -> copy(constitution = value)
    Ability.INT -> copy(intelligence = value)
    Ability.WIS -> copy(wisdom = value)
    Ability.CHA -> copy(charisma = value)
}

private fun Character.cycleSkillProf(skill: Skill): Character {
    val current = proficiencies[skill]
    val next = current.next()
    val newSkills = if (next == ProficiencyLevel.NONE) proficiencies.skills - skill
    else proficiencies.skills + (skill to next)
    return copy(proficiencies = proficiencies.copy(skills = newSkills))
}

private fun Character.cycleSaveProf(ability: Ability): Character {
    val current = proficiencies[ability]
    val next = current.next()
    val newSaves = if (next == ProficiencyLevel.NONE) proficiencies.saves - ability
    else proficiencies.saves + (ability to next)
    return copy(proficiencies = proficiencies.copy(saves = newSaves))
}

private fun ProficiencyLevel.next(): ProficiencyLevel = when (this) {
    ProficiencyLevel.NONE -> ProficiencyLevel.HALF
    ProficiencyLevel.HALF -> ProficiencyLevel.PROFICIENT
    ProficiencyLevel.PROFICIENT -> ProficiencyLevel.EXPERTISE
    ProficiencyLevel.EXPERTISE -> ProficiencyLevel.NONE
}

/**
 * Bumps the level on the named class entry by [delta] (clamped to 1..20),
 * and clamps the *total* character level to 20 so multiclass leveling
 * doesn't blow past the cap mid-press.
 */
private fun Character.adjustLevel(className: String, delta: Int): Character {
    val totalAfter = (totalLevel + delta).coerceIn(0, 20)
    val realDelta = totalAfter - totalLevel
    if (realDelta == 0) return this
    return copy(
        classes = classes.map {
            if (it.className == className) {
                it.copy(level = (it.level + realDelta).coerceIn(1, 20))
            } else it
        }
    )
}

private fun Character.removeClass(className: String): Character =
    copy(classes = classes.filterNot { it.className == className })

@Composable
private fun vmFromContainer(characterId: String): CharacterOverviewViewModel {
    val container = (LocalContext.current.applicationContext as DnDApplication).container
    return viewModel(
        factory = CharacterOverviewViewModel.factory(characterId, container.characterRepository),
        key = "overview:$characterId"
    )
}
