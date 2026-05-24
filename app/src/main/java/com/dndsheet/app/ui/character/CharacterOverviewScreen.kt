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
import androidx.compose.material3.TextButton
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
import com.dndsheet.app.ui.character.components.SheetBox
import com.dndsheet.app.ui.character.components.StatRow
import com.dndsheet.app.ui.character.layout.BoxId
import com.dndsheet.app.ui.character.layout.SheetCanvas
import com.dndsheet.app.ui.character.layout.defaultRows
import com.dndsheet.app.ui.character.layout.draggableBox
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
import com.dndsheet.domain.model.BoxPosition
import com.dndsheet.domain.model.Character
import com.dndsheet.domain.model.ClassLevel
import com.dndsheet.domain.model.SheetLayout
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
                    },
                    onSpendHitDie = { die ->
                        viewModel.update { it.adjustHitDie(die, -1) }
                    },
                    onRestoreHitDie = { die ->
                        viewModel.update { it.adjustHitDie(die, +1) }
                    },
                    onRestoreAllHitDice = {
                        viewModel.update { it.copy(hitDiceRemaining = emptyMap()) }
                    },
                    onPersistLayout = { positions ->
                        viewModel.update { it.copy(layout = SheetLayout(positions)) }
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
    onRemoveClass: (className: String) -> Unit,
    onSpendHitDie: (dieSize: Int) -> Unit,
    onRestoreHitDie: (dieSize: Int) -> Unit,
    onRestoreAllHitDice: () -> Unit,
    onPersistLayout: (Map<String, BoxPosition>) -> Unit
) {
    val pb = ProficiencyCalculator.bonus(character)
    val pbOver = ProficiencyCalculator.isOverridden(character)
    val initiative = PassiveCalculator.initiative(character)
    val initOver = character.overrides.initiative != null

    // The canvas reads positions from local state during a drag so each frame
    // is cheap; the persisted character is only touched once per drag (on
    // release) via onCommit. Reset whenever the character id changes — and
    // re-sync from the persisted layout when it changes underneath us (a save
    // bumps revision), so an external edit doesn't get stranded.
    var workingPositions by remember(character.id) {
        mutableStateOf(character.layout.positions)
    }
    androidx.compose.runtime.LaunchedEffect(character.layout.positions) {
        workingPositions = character.layout.positions
    }

    val onMove: (BoxId, Float, Float) -> Unit = { id, x, y ->
        val prev = workingPositions[id.name] ?: BoxPosition()
        // Clamp to canvas-coordinate floor so a box can't be dragged above the
        // canvas top or off the left edge. In edit mode the identity/class editor
        // sections push the canvas down, giving the user room to drag a box into
        // that space; without clamping the stored y goes negative, which leaves
        // the box above the viewport once those sections collapse in read mode.
        workingPositions = workingPositions + (id.name to prev.copy(
            x = x.coerceAtLeast(0f),
            y = y.coerceAtLeast(0f)
        ))
    }
    val onCommit: () -> Unit = { onPersistLayout(workingPositions) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Identity and class editors are editing tools, not sheet boxes, so
        // they live above the canvas and aren't part of the draggable layout.
        // Crucially, keeping them out of the canvas means the canvas geometry
        // is identical in edit and read mode — so a box dragged to an absolute
        // position lands in the same spot in both modes (an in-canvas editor
        // that grew/shrank with the mode would shift moved boxes by its height
        // difference).
        if (editing) {
            IdentityEditorSection(
                character = character,
                onOpenDialog = onOpenDialog
            )
            ClassEditorSection(
                classes = character.classes,
                onAdd = { onOpenDialog(ActiveDialog.AddClass) },
                onLevel = onLevel,
                onRemove = onRemoveClass
            )
        }

        SheetCanvas(
            positions = workingPositions,
            rows = defaultRows(character.ruleset),
            modifier = Modifier.fillMaxWidth()
        ) {
            HeaderBlock(
                character = character,
                modifier = Modifier.draggableBox(BoxId.HEADER, editing, onMove, onCommit)
            )

            VitalChip("Level", character.totalLevel.toString(), false,
                modifier = Modifier.draggableBox(BoxId.VITALS_LEVEL, editing, onMove, onCommit))
            VitalChip("Prof", formatModifier(pb), pbOver,
                modifier = Modifier.draggableBox(BoxId.VITALS_PROF, editing, onMove, onCommit))
            VitalChip("Init", formatModifier(initiative), initOver,
                modifier = Modifier.draggableBox(BoxId.VITALS_INIT, editing, onMove, onCommit))
            HpBox(
                current = character.currentHp,
                max = character.maxHp,
                modifier = Modifier.draggableBox(BoxId.VITALS_HP, editing, onMove, onCommit),
                onEdit = if (editing) ({ onOpenDialog(ActiveDialog.EditHp) }) else null,
                onDamage = { onOpenDialog(ActiveDialog.AdjustHp(isHeal = false)) },
                onHeal = { onOpenDialog(ActiveDialog.AdjustHp(isHeal = true)) }
            )

            if (character.classes.isNotEmpty()) {
                HitDiceBox(
                    character = character,
                    onSpend = onSpendHitDie,
                    onRestore = onRestoreHitDie,
                    onRestoreAll = onRestoreAllHitDice,
                    modifier = Modifier.draggableBox(BoxId.HIT_DICE, editing, onMove, onCommit)
                )
            }

            Ability.entries.forEach { ability ->
                AbilityScoreBlock(
                    label = ability.abbr,
                    score = character.abilityScores[ability],
                    abilityMod = AbilityCalculator.modifier(character, ability),
                    isOverridden = AbilityCalculator.isOverridden(character, ability),
                    modifier = Modifier.draggableBox(abilityBoxId(ability), editing, onMove, onCommit),
                    onClick = if (editing) ({ onOpenDialog(ActiveDialog.EditAbility(ability)) }) else null
                )
            }

            SavesBoxes(character, editing, onCycleSaveProf, onMove, onCommit)
            SkillsBoxes(character, editing, onCycleSkillProf, onMove, onCommit)

            SheetBox(
                title = "Passives",
                modifier = Modifier.draggableBox(BoxId.PASSIVES, editing, onMove, onCommit)
            ) {
                PassivesSection(character)
            }

            if (character.conditions.isNotEmpty()) {
                SheetBox(
                    title = "Conditions",
                    modifier = Modifier.draggableBox(BoxId.CONDITIONS, editing, onMove, onCommit)
                ) {
                    Text(
                        text = character.conditions.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

private fun abilityBoxId(a: Ability): BoxId = when (a) {
    Ability.STR -> BoxId.ABILITY_STR
    Ability.DEX -> BoxId.ABILITY_DEX
    Ability.CON -> BoxId.ABILITY_CON
    Ability.INT -> BoxId.ABILITY_INT
    Ability.WIS -> BoxId.ABILITY_WIS
    Ability.CHA -> BoxId.ABILITY_CHA
}

private fun saveBoxId(a: Ability): BoxId = when (a) {
    Ability.STR -> BoxId.SAVE_STR
    Ability.DEX -> BoxId.SAVE_DEX
    Ability.CON -> BoxId.SAVE_CON
    Ability.INT -> BoxId.SAVE_INT
    Ability.WIS -> BoxId.SAVE_WIS
    Ability.CHA -> BoxId.SAVE_CHA
}

private fun skillsBoxId(a: Ability): BoxId = when (a) {
    Ability.STR -> BoxId.SKILLS_STR
    Ability.DEX -> BoxId.SKILLS_DEX
    Ability.INT -> BoxId.SKILLS_INT
    Ability.WIS -> BoxId.SKILLS_WIS
    Ability.CHA -> BoxId.SKILLS_CHA
    Ability.CON -> error("CON has no skills box")
}

/**
 * The in-canvas identity box. Renders the same compact display in both edit
 * and read mode so the box never changes height — that height stability is
 * what lets dragged boxes keep their absolute positions across an edit-mode
 * toggle. Identity *editing* lives in [IdentityEditorSection], above the
 * canvas, alongside the class editor.
 */
@Composable
private fun HeaderBlock(
    character: Character,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
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

/**
 * Identity editing tool shown above the canvas in edit mode. Mirrors the
 * [ClassEditorSection] pattern: it's not a sheet box and isn't draggable, so
 * it can grow as tall as it needs without affecting the canvas layout. The
 * name field also drives the title bar, so edits there update everywhere.
 */
@Composable
private fun IdentityEditorSection(
    character: Character,
    onOpenDialog: (ActiveDialog) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionHeader("Identity")
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

/**
 * HP is treated as one unit: label, current/max value, and the two heal/damage
 * buttons all live inside the same box so the box acts as a single draggable
 * element (commit 6). The − and + buttons are always tappable; the value area
 * is only tappable in edit mode (opens the full max/current dialog). Compose's
 * nested-clickable semantics let both targets coexist — the buttons consume
 * their own clicks before the outer area sees them.
 */
@Composable
private fun HpBox(
    current: Int,
    max: Int,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
    onDamage: () -> Unit,
    onHeal: () -> Unit
) {
    val click = if (onEdit != null) Modifier.clickable(onClick = onEdit) else Modifier
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
            .then(click)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("HP", style = MaterialTheme.typography.labelSmall)
            Text(
                "$current/$max",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedButton(
                    onClick = onDamage,
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp)
                ) { Text("−", style = MaterialTheme.typography.titleMedium) }
                OutlinedButton(
                    onClick = onHeal,
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp)
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

/**
 * Ruleset-aware saves. Emits canvas children directly (each tagged with a
 * BoxId) rather than wrapping them in its own Row/Column — the SheetCanvas
 * owns positioning. 5e: one box. 2024: six small boxes the canvas arranges
 * into a 3×2 grid via [defaultRows].
 */
@Composable
private fun SavesBoxes(
    character: Character,
    editing: Boolean,
    onCycleSaveProf: (Ability) -> Unit,
    onMove: (BoxId, Float, Float) -> Unit,
    onCommit: () -> Unit
) {
    if (character.ruleset == Ruleset.DND_5E_2014) {
        SheetBox(
            title = "Saving Throws",
            modifier = Modifier.draggableBox(BoxId.SAVES_ALL, editing, onMove, onCommit)
        ) {
            for (ability in Ability.entries) {
                SaveRow(character, ability, editing, onCycleSaveProf)
            }
        }
    } else {
        Ability.entries.forEach { ability ->
            SheetBox(
                title = "${ability.abbr} Save",
                modifier = Modifier.draggableBox(saveBoxId(ability), editing, onMove, onCommit),
                contentPadding = 8.dp
            ) {
                SaveRow(character, ability, editing, onCycleSaveProf, compact = true)
            }
        }
    }
}

@Composable
private fun SaveRow(
    character: Character,
    ability: Ability,
    editing: Boolean,
    onCycleSaveProf: (Ability) -> Unit,
    compact: Boolean = false
) {
    StatRow(
        label = if (compact) "" else ability.abbr,
        bonus = SavingThrowCalculator.bonus(character, ability),
        proficiencyTier = character.proficiencies[ability],
        isOverridden = SavingThrowCalculator.isOverridden(character, ability),
        onClick = if (editing) ({ onCycleSaveProf(ability) }) else null
    )
}

/**
 * Ruleset-aware skills. 5e: one box with all 18. 2024: one box per ability
 * (CON skipped — no skills). Each box is a direct canvas child.
 */
@Composable
private fun SkillsBoxes(
    character: Character,
    editing: Boolean,
    onCycleSkillProf: (Skill) -> Unit,
    onMove: (BoxId, Float, Float) -> Unit,
    onCommit: () -> Unit
) {
    if (character.ruleset == Ruleset.DND_5E_2014) {
        SheetBox(
            title = "Skills",
            modifier = Modifier.draggableBox(BoxId.SKILLS_ALL, editing, onMove, onCommit)
        ) {
            for (skill in Skill.entries) {
                SkillRow(character, skill, editing, onCycleSkillProf, showAbility = true)
            }
        }
    } else {
        val byAbility = Skill.entries.groupBy { it.ability }
        listOf(Ability.STR, Ability.DEX, Ability.INT, Ability.WIS, Ability.CHA).forEach { ab ->
            val skills = byAbility[ab].orEmpty()
            if (skills.isEmpty()) return@forEach
            SheetBox(
                title = "${ab.abbr} Skills",
                modifier = Modifier.draggableBox(skillsBoxId(ab), editing, onMove, onCommit)
            ) {
                skills.forEach { skill ->
                    SkillRow(character, skill, editing, onCycleSkillProf, showAbility = false)
                }
            }
        }
    }
}

@Composable
private fun SkillRow(
    character: Character,
    skill: Skill,
    editing: Boolean,
    onCycleSkillProf: (Skill) -> Unit,
    showAbility: Boolean
) {
    StatRow(
        label = if (showAbility) "${skill.display}  (${skill.ability.abbr})" else skill.display,
        bonus = SkillCalculator.bonus(character, skill),
        proficiencyTier = character.proficiencies[skill],
        isOverridden = SkillCalculator.isOverridden(character, skill),
        onClick = if (editing) ({ onCycleSkillProf(skill) }) else null
    )
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

/**
 * Per spec §3 hit dice live with the rest of the combat-relevant vitals.
 * Max per die size is derived from the character's classes (sum of levels
 * for classes sharing that hit die), and available comes from
 * `character.hitDiceRemaining`, defaulting to max when the map is missing
 * an entry. Storing only "spent" state in the map keeps freshly created
 * characters at full hit dice without an init migration.
 *
 * Both buttons are always tappable — short rests are play-time actions
 * that shouldn't require entering edit mode. The Reset affordance in the
 * header restores everything to max for long rests; finer long-rest
 * mechanics (half recovery, etc.) can land when the rest system grows.
 */
@Composable
private fun HitDiceBox(
    character: Character,
    onSpend: (dieSize: Int) -> Unit,
    onRestore: (dieSize: Int) -> Unit,
    onRestoreAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val maxByDie: Map<Int, Int> = character.classes
        .groupBy { it.hitDie }
        .mapValues { (_, classes) -> classes.sumOf { it.level } }

    val anySpent = maxByDie.any { (size, max) ->
        (character.hitDiceRemaining[size] ?: max) < max
    }

    SheetBox(modifier = modifier) {
        // Title row with a Reset affordance on the right that appears only
        // when anything is spent — keeps the box clean during normal play.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hit Dice",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (anySpent) {
                TextButton(onClick = onRestoreAll) { Text("Reset") }
            }
        }
        maxByDie.entries.sortedBy { it.key }.forEach { (dieSize, max) ->
            val available = character.hitDiceRemaining[dieSize] ?: max
            HitDieRow(
                dieSize = dieSize,
                available = available,
                max = max,
                onSpend = { onSpend(dieSize) },
                onRestore = { onRestore(dieSize) }
            )
        }
    }
}

@Composable
private fun HitDieRow(
    dieSize: Int,
    available: Int,
    max: Int,
    onSpend: () -> Unit,
    onRestore: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onSpend, enabled = available > 0) {
            Icon(Icons.Default.Remove, contentDescription = "Spend a d$dieSize")
        }
        Text(
            text = "$available / $max",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(64.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            text = "d$dieSize",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        IconButton(onClick = onRestore, enabled = available < max) {
            Icon(Icons.Default.Add, contentDescription = "Restore a d$dieSize")
        }
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

/**
 * Adjusts hit-dice remaining for a single die size by [delta] (−1 to spend,
 * +1 to restore). The map only stores values that diverge from "full" so
 * an untouched character keeps the map empty. Clamps to [0, max] where
 * max is the sum of class levels with this hit die.
 */
private fun Character.adjustHitDie(dieSize: Int, delta: Int): Character {
    val maxForDie = classes.filter { it.hitDie == dieSize }.sumOf { it.level }
    if (maxForDie == 0) return this
    val current = hitDiceRemaining[dieSize] ?: maxForDie
    val updated = (current + delta).coerceIn(0, maxForDie)
    if (updated == current) return this
    return copy(hitDiceRemaining = hitDiceRemaining + (dieSize to updated))
}

@Composable
private fun vmFromContainer(characterId: String): CharacterOverviewViewModel {
    val container = (LocalContext.current.applicationContext as DnDApplication).container
    return viewModel(
        factory = CharacterOverviewViewModel.factory(characterId, container.characterRepository),
        key = "overview:$characterId"
    )
}
