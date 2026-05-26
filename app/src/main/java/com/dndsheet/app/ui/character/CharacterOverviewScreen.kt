package com.dndsheet.app.ui.character

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
import com.dndsheet.app.ui.character.layout.EditableSheetBox
import com.dndsheet.app.ui.character.layout.LocalBoxFontScale
import com.dndsheet.app.ui.character.layout.MIN_BOX_HEIGHT_DP
import com.dndsheet.app.ui.character.layout.MIN_BOX_WIDTH_DP
import com.dndsheet.app.ui.character.layout.SheetCanvas
import com.dndsheet.app.ui.character.layout.defaultRows
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
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
                    },
                    onImportPdf = { path ->
                        viewModel.update { it.copy(pdfPath = path) }
                    },
                    onClearPdf = {
                        viewModel.update { it.copy(pdfPath = null) }
                    },
                    onTogglePassive = { skill ->
                        viewModel.update { c ->
                            val updated = if (skill in c.passiveSkills) c.passiveSkills - skill
                                          else c.passiveSkills + skill
                            c.copy(passiveSkills = updated)
                        }
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
    onPersistLayout: (Map<String, BoxPosition>) -> Unit,
    onImportPdf: (pdfPath: String) -> Unit,
    onClearPdf: () -> Unit,
    onTogglePassive: (Skill) -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    // Launch the system file picker for PDF files. On selection, copy the
    // file into the app's private storage so the reference stays stable even
    // if the user later moves or deletes the original.
    val importPdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val dest = File(context.filesDir, "pdfs/${character.id}.pdf")
            dest.parentFile?.mkdirs()
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            onImportPdf(dest.absolutePath)
        }
    }

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

    // widthDp / heightDp are nullable: null means "leave the stored value unchanged"
    // so a right-edge drag doesn't freeze the auto height, and vice versa.
    val onResize: (BoxId, Float?, Float?) -> Unit = { id, w, h ->
        val prev = workingPositions[id.name] ?: BoxPosition()
        workingPositions = workingPositions + (id.name to prev.copy(
            width  = w?.coerceAtLeast(MIN_BOX_WIDTH_DP)  ?: prev.width,
            height = h?.coerceAtLeast(MIN_BOX_HEIGHT_DP) ?: prev.height
        ))
    }

    // Z-change is a button tap, not a drag, so persist immediately.
    val onZChange: (BoxId, Int) -> Unit = { id, delta ->
        val prev = workingPositions[id.name] ?: BoxPosition()
        workingPositions = workingPositions + (id.name to prev.copy(z = prev.z + delta))
        onCommit()
    }

    // Font-scale is also a button tap — steps 0.1f, range 0.5..2.0, persisted immediately.
    val fontScaleFor: (BoxId) -> Float = { id -> workingPositions[id.name]?.fontScale ?: 1f }
    val onFontScale: (BoxId, Int) -> Unit = { id, delta ->
        val prev = workingPositions[id.name] ?: BoxPosition()
        val newScale = (prev.fontScale + delta * 0.1f).coerceIn(0.5f, 2.0f)
        workingPositions = workingPositions + (id.name to prev.copy(fontScale = newScale))
        onCommit()
    }

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
            PdfSection(
                pdfPath = character.pdfPath,
                onImportClick = { importPdfLauncher.launch("application/pdf") },
                onClearClick = onClearPdf
            )
        }

        // PDF background (if set) renders behind the boxes inside a shared
        // Box so both scroll together in the parent Column.
        Box(modifier = Modifier.fillMaxWidth()) {
            character.pdfPath?.let { path ->
                PdfBackground(pdfPath = path, modifier = Modifier.fillMaxWidth())
            }

        SheetCanvas(
            positions = workingPositions,
            rows = defaultRows(character.ruleset),
            modifier = Modifier.fillMaxWidth()
        ) {
            EditableSheetBox(BoxId.HEADER, editing, onMove, onResize, onZChange, onFontScale, fontScaleFor(BoxId.HEADER), onCommit) {
                HeaderBlock(character = character)
            }

            EditableSheetBox(BoxId.VITALS_LEVEL, editing, onMove, onResize, onZChange, onFontScale, fontScaleFor(BoxId.VITALS_LEVEL), onCommit) {
                VitalChip("Level", character.totalLevel.toString(), false)
            }
            EditableSheetBox(BoxId.VITALS_PROF, editing, onMove, onResize, onZChange, onFontScale, fontScaleFor(BoxId.VITALS_PROF), onCommit) {
                VitalChip("Prof", formatModifier(pb), pbOver)
            }
            EditableSheetBox(BoxId.VITALS_INIT, editing, onMove, onResize, onZChange, onFontScale, fontScaleFor(BoxId.VITALS_INIT), onCommit) {
                VitalChip("Init", formatModifier(initiative), initOver)
            }
            EditableSheetBox(BoxId.VITALS_HP, editing, onMove, onResize, onZChange, onFontScale, fontScaleFor(BoxId.VITALS_HP), onCommit) {
                HpBox(
                    current = character.currentHp,
                    max = character.maxHp,
                    onEdit = if (editing) ({ onOpenDialog(ActiveDialog.EditHp) }) else null,
                    onDamage = { onOpenDialog(ActiveDialog.AdjustHp(isHeal = false)) },
                    onHeal = { onOpenDialog(ActiveDialog.AdjustHp(isHeal = true)) }
                )
            }

            if (character.classes.isNotEmpty()) {
                EditableSheetBox(BoxId.HIT_DICE, editing, onMove, onResize, onZChange, onFontScale, fontScaleFor(BoxId.HIT_DICE), onCommit) {
                    HitDiceBox(
                        character = character,
                        onSpend = onSpendHitDie,
                        onRestore = onRestoreHitDie,
                        onRestoreAll = onRestoreAllHitDice
                    )
                }
            }

            Ability.entries.forEach { ability ->
                EditableSheetBox(abilityBoxId(ability), editing, onMove, onResize, onZChange, onFontScale, fontScaleFor(abilityBoxId(ability)), onCommit) {
                    AbilityScoreBlock(
                        label = ability.abbr,
                        score = character.abilityScores[ability],
                        abilityMod = AbilityCalculator.modifier(character, ability),
                        isOverridden = AbilityCalculator.isOverridden(character, ability),
                        onClick = if (editing) ({ onOpenDialog(ActiveDialog.EditAbility(ability)) }) else null
                    )
                }
            }

            SavesBoxes(character, editing, onCycleSaveProf, onCycleSkillProf, onMove, onResize, onZChange, onFontScale, fontScaleFor, onCommit)
            // 2024 skills are rendered inside SavesBoxes (combined per-ability box)
            if (character.ruleset == Ruleset.DND_5E_2014) {
                SkillsBoxes(character, editing, onCycleSkillProf, onMove, onResize, onZChange, onFontScale, fontScaleFor, onCommit)
            }

            EditableSheetBox(BoxId.PASSIVES, editing, onMove, onResize, onZChange, onFontScale, fontScaleFor(BoxId.PASSIVES), onCommit) {
                SheetBox(title = "Passives") {
                    PassivesSection(character, editing, onTogglePassive)
                }
            }

            if (character.conditions.isNotEmpty()) {
                EditableSheetBox(BoxId.CONDITIONS, editing, onMove, onResize, onZChange, onFontScale, fontScaleFor(BoxId.CONDITIONS), onCommit) {
                    SheetBox(title = "Conditions") {
                        Text(
                            text = character.conditions.joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        } // end PDF+canvas Box
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
    SheetBox(modifier = modifier) {
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
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Text(
            text = character.ruleset.display,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 2.dp)
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
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
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
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
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
    onCycleSkillProf: (Skill) -> Unit,
    onMove: (BoxId, Float, Float) -> Unit,
    onResize: (BoxId, Float?, Float?) -> Unit,
    onZChange: (BoxId, Int) -> Unit,
    onFontScale: (BoxId, Int) -> Unit,
    fontScaleFor: (BoxId) -> Float,
    onCommit: () -> Unit
) {
    if (character.ruleset == Ruleset.DND_5E_2014) {
        EditableSheetBox(BoxId.SAVES_ALL, editing, onMove, onResize, onZChange, onFontScale, fontScaleFor(BoxId.SAVES_ALL), onCommit) {
            SheetBox(title = "Saving Throws") {
                for (ability in Ability.entries) {
                    SaveRow(character, ability, editing, onCycleSaveProf)
                }
            }
        }
    } else {
        // 2024: one box per ability — save row + divider + skill rows.
        // Uses SAVE_* BoxIds; SKILLS_* ids are retired from the default flow.
        val byAbility = Skill.entries.groupBy { it.ability }
        Ability.entries.forEach { ability ->
            val skills = byAbility[ability].orEmpty()
            EditableSheetBox(saveBoxId(ability), editing, onMove, onResize, onZChange, onFontScale, fontScaleFor(saveBoxId(ability)), onCommit) {
                SheetBox(title = ability.abbr, contentPadding = 8.dp) {
                    StatRow(
                        label = "Save",
                        bonus = SavingThrowCalculator.bonus(character, ability),
                        proficiencyTier = character.proficiencies[ability],
                        isOverridden = SavingThrowCalculator.isOverridden(character, ability),
                        onClick = if (editing) ({ onCycleSaveProf(ability) }) else null
                    )
                    if (skills.isNotEmpty()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 2.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        )
                        skills.forEach { skill ->
                            SkillRow(character, skill, editing, onCycleSkillProf, showAbility = false)
                        }
                    }
                }
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
    onResize: (BoxId, Float?, Float?) -> Unit,
    onZChange: (BoxId, Int) -> Unit,
    onFontScale: (BoxId, Int) -> Unit,
    fontScaleFor: (BoxId) -> Float,
    onCommit: () -> Unit
) {
    if (character.ruleset == Ruleset.DND_5E_2014) {
        EditableSheetBox(BoxId.SKILLS_ALL, editing, onMove, onResize, onZChange, onFontScale, fontScaleFor(BoxId.SKILLS_ALL), onCommit) {
            SheetBox(title = "Skills") {
                for (skill in Skill.entries) {
                    SkillRow(character, skill, editing, onCycleSkillProf, showAbility = true)
                }
            }
        }
    } else {
        val byAbility = Skill.entries.groupBy { it.ability }
        listOf(Ability.STR, Ability.DEX, Ability.INT, Ability.WIS, Ability.CHA).forEach { ab ->
            val skills = byAbility[ab].orEmpty()
            if (skills.isEmpty()) return@forEach
            EditableSheetBox(skillsBoxId(ab), editing, onMove, onResize, onZChange, onFontScale, fontScaleFor(skillsBoxId(ab)), onCommit) {
                SheetBox(title = "${ab.abbr} Skills") {
                    skills.forEach { skill ->
                        SkillRow(character, skill, editing, onCycleSkillProf, showAbility = false)
                    }
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

/**
 * Passives box content.
 *
 * **View mode**: shows only the skills the user has selected, sorted
 * alphabetically, rendered as regular [StatRow]s (proficiency dot visible so
 * the player understands why the number is what it is).
 *
 * **Edit mode**: shows every skill. Selected ones appear at the top
 * (alphabetically), unselected below (also alphabetically) — this gives a
 * live preview of how the box will look once edit mode is exited. Each row is
 * tappable to toggle selection; selected rows are full-opacity, unselected rows
 * are dimmed. The leading circle acts as a visual checkbox.
 */
@Composable
private fun PassivesSection(
    character: Character,
    editing: Boolean,
    onToggle: (Skill) -> Unit
) {
    val selectedSet = character.passiveSkills.toSet()

    if (editing) {
        val (sel, unsel) = Skill.entries.partition { it in selectedSet }
        val sorted = sel.sortedBy { it.display } + unsel.sortedBy { it.display }
        Column {
            sorted.forEach { skill ->
                PassiveToggleRow(
                    skill       = skill,
                    passiveScore = PassiveCalculator.passive(character, skill),
                    isSelected  = skill in selectedSet,
                    onClick     = { onToggle(skill) }
                )
            }
        }
    } else {
        val overrideFor: (Skill) -> Boolean = { skill -> when (skill) {
            Skill.PERCEPTION    -> character.overrides.passivePerception    != null
            Skill.INVESTIGATION -> character.overrides.passiveInvestigation != null
            Skill.INSIGHT       -> character.overrides.passiveInsight       != null
            else                -> false
        }}
        Column {
            character.passiveSkills
                .sortedBy { it.display }
                .forEach { skill ->
                    StatRow(
                        label           = "Passive ${skill.display}",
                        bonus           = PassiveCalculator.passive(character, skill),
                        proficiencyTier = character.proficiencies[skill],
                        isOverridden    = overrideFor(skill)
                    )
                }
        }
    }
}

/**
 * Single row in the edit-mode passive picker. The leading circle acts as a
 * checkbox: filled = will appear on the sheet, outline = hidden in view mode.
 * Unselected rows are dimmed so the selected ones read as the "real" list.
 */
@Composable
private fun PassiveToggleRow(
    skill: Skill,
    passiveScore: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale      = LocalBoxFontScale.current
    val slotSize   = (16.dp * scale).coerceAtLeast(10.dp)
    val dotSize    = (12.dp * scale).coerceAtLeast(8.dp)
    val vertPad    = (3.dp  * scale).coerceAtLeast(1.dp)
    val contentAlpha = if (isSelected) 1f else 0.38f

    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    val onSurface = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = vertPad),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Selection dot — matches ProficiencyDot geometry so columns line up
        Box(modifier = Modifier.size(slotSize), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .background(
                        color  = if (isSelected) primary else Color.Transparent,
                        shape  = CircleShape
                    )
                    .border(
                        width  = 1.dp,
                        color  = if (isSelected) primary else outline,
                        shape  = CircleShape
                    )
            )
        }
        Text(
            text     = "Passive ${skill.display}",
            style    = MaterialTheme.typography.bodySmall,
            color    = onSurface.copy(alpha = contentAlpha),
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f)
        )
        Text(
            text       = passiveScore.toString(),
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color      = onSurface.copy(alpha = contentAlpha)
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
    val scale = LocalBoxFontScale.current
    val buttonSize  = (32.dp * scale).coerceAtLeast(20.dp)
    val iconSize    = (16.dp * scale).coerceAtLeast(10.dp)
    val valueWidth  = (52.dp * scale).coerceAtLeast(32.dp)
    val verticalPad = (2.dp  * scale).coerceAtLeast(1.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = verticalPad),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onSpend,
            enabled = available > 0,
            modifier = Modifier.size(buttonSize)
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Spend a d$dieSize",
                modifier = Modifier.size(iconSize))
        }
        Text(
            text = "$available / $max",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(valueWidth),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            text = "d$dieSize",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        IconButton(
            onClick = onRestore,
            enabled = available < max,
            modifier = Modifier.size(buttonSize)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Restore a d$dieSize",
                modifier = Modifier.size(iconSize))
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

/**
 * Edit-mode section for managing the PDF background. Shows the import button
 * and, when a PDF is already set, a "Clear" affordance. Sits above the canvas
 * in the same column as IdentityEditorSection and ClassEditorSection, so it
 * is never part of the draggable layout.
 */
@Composable
private fun PdfSection(
    pdfPath: String?,
    onImportClick: () -> Unit,
    onClearClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionHeader("PDF Background")
        if (pdfPath != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "PDF imported",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                TextButton(onClick = onClearClick) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            }
        }
        OutlinedButton(
            onClick = onImportClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (pdfPath != null) "Replace PDF" else "Import PDF")
        }
    }
}

/**
 * Renders all pages of the PDF at [pdfPath] stacked vertically, each scaled
 * to fill the full screen width. Pages are rendered on [Dispatchers.IO] the
 * first time (and whenever [pdfPath] changes) and then displayed as bitmaps.
 *
 * Keeping the render result in [produceState] means Compose reuses the
 * bitmaps across recompositions — we only re-render when the path changes.
 */
@Composable
private fun PdfBackground(pdfPath: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val pages by produceState<List<Bitmap>>(initialValue = emptyList(), key1 = pdfPath) {
        value = withContext(Dispatchers.IO) { renderPdfPages(context, pdfPath) }
    }
    Column(modifier = modifier) {
        pages.forEach { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth
            )
        }
    }
}

/**
 * Renders every page of the PDF file at [pdfPath] into [Bitmap]s sized to
 * the device's screen width. Must be called from a background thread.
 * Returns an empty list if the file doesn't exist.
 */
private fun renderPdfPages(context: Context, pdfPath: String): List<Bitmap> {
    val file = File(pdfPath)
    if (!file.exists()) return emptyList()
    val screenWidth = context.resources.displayMetrics.widthPixels
    val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    val renderer = PdfRenderer(fd)
    val bitmaps = mutableListOf<Bitmap>()
    try {
        repeat(renderer.pageCount) { i ->
            val page = renderer.openPage(i)
            val scale = screenWidth.toFloat() / page.width
            val h = (page.height * scale).toInt()
            val bmp = Bitmap.createBitmap(screenWidth, h, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(android.graphics.Color.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            bitmaps += bmp
        }
    } finally {
        renderer.close()
        fd.close()
    }
    return bitmaps
}

@Composable
private fun vmFromContainer(characterId: String): CharacterOverviewViewModel {
    val container = (LocalContext.current.applicationContext as DnDApplication).container
    return viewModel(
        factory = CharacterOverviewViewModel.factory(characterId, container.characterRepository),
        key = "overview:$characterId"
    )
}
