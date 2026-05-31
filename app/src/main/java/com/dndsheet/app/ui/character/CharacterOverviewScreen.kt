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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
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
import com.dndsheet.app.ui.character.layout.pinchToZoom
import com.dndsheet.app.ui.character.layout.SheetCanvas
import com.dndsheet.app.ui.character.layout.defaultRows
import com.dndsheet.app.ui.character.edit.ActiveDialog
import com.dndsheet.app.ui.character.edit.AddClassDialog
import com.dndsheet.app.ui.character.edit.AlignmentDialog
import com.dndsheet.app.ui.character.edit.HpAdjustDialog
import com.dndsheet.app.ui.character.edit.HpDialog
import com.dndsheet.app.ui.character.edit.TempHpDialog
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
import com.dndsheet.rules.HpCalculator
import com.dndsheet.rules.PassiveCalculator
import com.dndsheet.rules.ProficiencyCalculator
import com.dndsheet.rules.SavingThrowCalculator
import com.dndsheet.rules.SkillCalculator
import com.dndsheet.rules.SpellcastingCalculator
import com.dndsheet.rules.SpellcastingRow
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateListOf
import com.dndsheet.app.ui.character.layout.DEFAULT_INK_COLOR
import com.dndsheet.app.ui.character.layout.DEFAULT_INK_WIDTH
import com.dndsheet.app.ui.character.layout.InkCanvas
import com.dndsheet.app.ui.character.layout.InkTool
import com.dndsheet.app.ui.character.layout.InkToolbar
import com.dndsheet.domain.model.Stroke

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
    var inkMode by remember { mutableStateOf(false) }
    var activeDialog by remember { mutableStateOf<ActiveDialog>(ActiveDialog.None) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = when {
                    editing -> TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                    inkMode -> TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                    else    -> TopAppBarDefaults.topAppBarColors()
                },
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
                    // Ink mode — disabled while edit mode is active.
                    IconButton(
                        onClick  = { inkMode = !inkMode; if (inkMode) { editing = false; activeDialog = ActiveDialog.None } },
                        enabled  = !editing
                    ) {
                        if (inkMode) Icon(Icons.Default.Check, contentDescription = "Done inking")
                        else         Icon(Icons.Default.Draw,  contentDescription = "Ink mode")
                    }
                    // Edit mode — disabled while ink mode is active.
                    IconButton(
                        onClick  = { editing = !editing; activeDialog = ActiveDialog.None; if (editing) inkMode = false },
                        enabled  = !inkMode
                    ) {
                        if (editing) Icon(Icons.Default.Check, contentDescription = "Done editing")
                        else         Icon(Icons.Default.Edit,  contentDescription = "Edit")
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
                    inkMode = inkMode,
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
                    onPersistLayout = { layout ->
                        viewModel.update { it.copy(layout = layout) }
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
                    },
                    onSaveStrokes = { strokes ->
                        viewModel.update { it.copy(inkStrokes = strokes) }
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
                    if (active.isHeal) {
                        // Healing restores current HP only (never temp HP),
                        // capped at max.
                        it.copy(currentHp = HpCalculator.heal(it.currentHp, it.maxHp, amount))
                    } else {
                        // Damage depletes temporary HP first, then current HP.
                        val result = HpCalculator.takeDamage(it.currentHp, it.temporaryHp, amount)
                        it.copy(currentHp = result.currentHp, temporaryHp = result.temporaryHp)
                    }
                }
            }
        )

        ActiveDialog.AddTempHp -> TempHpDialog(
            current = character.temporaryHp,
            onDismiss = onDismiss,
            onAdd = { amount ->
                // Temp HP doesn't stack: keep the greater of the two pools.
                onApply { it.copy(temporaryHp = HpCalculator.applyTemporaryHp(it.temporaryHp, amount)) }
            },
            onClear = { onApply { it.copy(temporaryHp = 0) } }
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
    inkMode: Boolean,
    onOpenDialog: (ActiveDialog) -> Unit,
    onCycleSkillProf: (Skill) -> Unit,
    onCycleSaveProf: (Ability) -> Unit,
    onLevel: (className: String, delta: Int) -> Unit,
    onRemoveClass: (className: String) -> Unit,
    onSpendHitDie: (dieSize: Int) -> Unit,
    onRestoreHitDie: (dieSize: Int) -> Unit,
    onRestoreAllHitDice: () -> Unit,
    onPersistLayout: (SheetLayout) -> Unit,
    onImportPdf: (pdfPath: String) -> Unit,
    onClearPdf: () -> Unit,
    onTogglePassive: (Skill) -> Unit,
    onSaveStrokes: (List<Stroke>) -> Unit
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
            val dir = File(context.filesDir, "pdfs")
            dir.mkdirs()
            // Remove any previously imported PDF(s) for this character. Using a
            // unique (timestamped) destination filename is what makes "Replace"
            // work: the pdfPath string actually changes, so the character state
            // updates and PdfBackground re-renders. Writing to a fixed path kept
            // the same string and the old pages stayed cached.
            dir.listFiles { f ->
                f.name.startsWith(character.id) && f.name.endsWith(".pdf")
            }?.forEach { it.delete() }
            val dest = File(dir, "${character.id}-${System.currentTimeMillis()}.pdf")
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

    // Reference width (dp) the stored positions are expressed in. Captured the
    // first time a box is moved/resized (= the canvas width at that moment) and
    // persisted so positions can be scaled back to whatever width the device is
    // at on a later open/rotation, keeping boxes aligned to the background.
    var workingReferenceWidth by remember(character.id) {
        mutableStateOf(character.layout.referenceWidthDp)
    }
    androidx.compose.runtime.LaunchedEffect(character.layout.referenceWidthDp) {
        workingReferenceWidth = character.layout.referenceWidthDp
    }

    // Live canvas/background width in px (the zoom Box fills the canvas width).
    // Declared here so the drag handlers below can convert on-screen drag
    // coordinates into the stored reference frame.
    val density = LocalDensity.current
    var zoomBoxSize by remember { mutableStateOf(IntSize.Zero) }
    // Current canvas width in dp, or 0 before the first layout pass.
    fun canvasWidthDp(): Float =
        if (zoomBoxSize.width > 0) with(density) { zoomBoxSize.width.toDp().value } else 0f
    // Converts an on-screen dp value into the stored reference frame, lazily
    // establishing the reference width on first use.
    fun toReferenceDp(screenDp: Float): Float {
        val cw = canvasWidthDp()
        if (workingReferenceWidth <= 0f && cw > 0f) workingReferenceWidth = cw
        val refW = if (workingReferenceWidth > 0f) workingReferenceWidth else cw
        return if (cw > 0f) screenDp * (refW / cw) else screenDp
    }
    // Ink is persisted separately from the layout, so a character that's only
    // ever drawn on (never has a box moved) would otherwise never capture a
    // reference width. Establish it from the current canvas width on the first
    // coordinate-bearing ink edit and persist the layout so the scale survives
    // a reload/rotation. Strokes committed at this moment are already in the
    // reference frame (reference width == the width they were drawn at).
    fun ensureReferenceWidth() {
        val cw = canvasWidthDp()
        if (workingReferenceWidth <= 0f && cw > 0f) {
            workingReferenceWidth = cw
            onPersistLayout(SheetLayout(workingPositions, workingReferenceWidth))
        }
    }

    val onMove: (BoxId, Float, Float) -> Unit = { id, x, y ->
        val prev = workingPositions[id.name] ?: BoxPosition()
        // Clamp to canvas-coordinate floor so a box can't be dragged above the
        // canvas top or off the left edge. In edit mode the identity/class editor
        // sections push the canvas down, giving the user room to drag a box into
        // that space; without clamping the stored y goes negative, which leaves
        // the box above the viewport once those sections collapse in read mode.
        workingPositions = workingPositions + (id.name to prev.copy(
            x = toReferenceDp(x.coerceAtLeast(0f)),
            y = toReferenceDp(y.coerceAtLeast(0f))
        ))
    }
    val onCommit: () -> Unit = {
        onPersistLayout(SheetLayout(workingPositions, workingReferenceWidth))
    }

    // widthDp / heightDp are nullable: null means "leave the stored value unchanged"
    // so a right-edge drag doesn't freeze the auto height, and vice versa.
    // Incoming values are on-screen dp; convert to the stored reference frame.
    val onResize: (BoxId, Float?, Float?) -> Unit = { id, w, h ->
        val prev = workingPositions[id.name] ?: BoxPosition()
        workingPositions = workingPositions + (id.name to prev.copy(
            width  = w?.let { toReferenceDp(it.coerceAtLeast(MIN_BOX_WIDTH_DP)) }  ?: prev.width,
            height = h?.let { toReferenceDp(it.coerceAtLeast(MIN_BOX_HEIGHT_DP)) } ?: prev.height
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

    // ── Ink layer state ───────────────────────────────────────────────────────
    var workingStrokes by remember(character.id) { mutableStateOf(character.inkStrokes) }
    androidx.compose.runtime.LaunchedEffect(character.inkStrokes) {
        workingStrokes = character.inkStrokes
    }
    var activeTool by remember { mutableStateOf(InkTool.PEN) }
    var penColor   by remember { mutableStateOf(DEFAULT_INK_COLOR) }
    var penWidth   by remember { mutableStateOf(DEFAULT_INK_WIDTH) }

    // ── Zoom state ──────────────────────────────────────────────────────────────
    // Pinch-to-zoom is applied as a single graphicsLayer transform wrapping every
    // sheet layer (PDF, boxes, ink) so they scale and pan as one rigid piece.
    // Transient view state — reset per character, not persisted.
    var zoomScale  by remember(character.id) { mutableStateOf(1f) }
    var zoomOffset by remember(character.id) { mutableStateOf(Offset.Zero) }
    // zoomBoxSize is declared earlier (drag handlers read the canvas width).

    // SnapshotStateList so the toolbar can observe .size for undo/redo enabled state.
    val undoStack = remember { mutableStateListOf<List<Stroke>>() }
    val redoStack = remember { mutableStateListOf<List<Stroke>>() }

    val onStrokeComplete: (Stroke) -> Unit = { stroke ->
        ensureReferenceWidth()
        undoStack.add(workingStrokes)
        if (undoStack.size > 50) undoStack.removeAt(0)
        redoStack.clear()
        workingStrokes = workingStrokes + stroke
        onSaveStrokes(workingStrokes)
    }
    val onErase: (Set<String>) -> Unit = { erasedIds ->
        if (erasedIds.isNotEmpty()) {
            undoStack.add(workingStrokes)
            if (undoStack.size > 50) undoStack.removeAt(0)
            redoStack.clear()
            workingStrokes = workingStrokes.filter { it.id !in erasedIds }
            onSaveStrokes(workingStrokes)
        }
    }
    val onUndo: () -> Unit = {
        if (undoStack.isNotEmpty()) {
            redoStack.add(workingStrokes)
            workingStrokes = undoStack.removeLast()
            onSaveStrokes(workingStrokes)
        }
    }
    val onRedo: () -> Unit = {
        if (redoStack.isNotEmpty()) {
            undoStack.add(workingStrokes)
            workingStrokes = redoStack.removeLast()
            onSaveStrokes(workingStrokes)
        }
    }

    // ── Selection callbacks ───────────────────────────────────────────────────
    var clipboardStrokes by remember { mutableStateOf(emptyList<Stroke>()) }

    fun pushUndo() {
        undoStack.add(workingStrokes)
        if (undoStack.size > 50) undoStack.removeAt(0)
        redoStack.clear()
    }
    val onSelectionMove: (List<Stroke>) -> Unit = { moved ->
        pushUndo()
        val ids = moved.map { it.id }.toSet()
        workingStrokes = workingStrokes.map { s -> moved.find { it.id == s.id } ?: s }
        onSaveStrokes(workingStrokes)
    }
    val onSelectionDelete: (Set<String>) -> Unit = { ids ->
        pushUndo()
        workingStrokes = workingStrokes.filter { it.id !in ids }
        onSaveStrokes(workingStrokes)
    }
    val onCopy: (List<Stroke>) -> Unit = { selected ->
        clipboardStrokes = selected
    }
    val onCut: (Set<String>) -> Unit = { ids ->
        clipboardStrokes = workingStrokes.filter { it.id in ids }
        pushUndo()
        workingStrokes = workingStrokes.filter { it.id !in ids }
        onSaveStrokes(workingStrokes)
    }
    val onPaste: (List<Stroke>) -> Unit = { toInsert ->
        pushUndo()
        workingStrokes = workingStrokes + toInsert
        onSaveStrokes(workingStrokes)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // InkToolbar is fixed above the scrollable content — it stays pinned
        // while the sheet scrolls beneath it.
        AnimatedVisibility(visible = inkMode) {
            InkToolbar(
                activeTool    = activeTool,
                onToolChange  = { activeTool = it },
                penColor      = penColor,
                onColorChange = { penColor = it },
                penWidth      = penWidth,
                onWidthChange = { penWidth = it },
                canUndo       = undoStack.isNotEmpty(),
                canRedo       = redoStack.isNotEmpty(),
                onUndo        = onUndo,
                onRedo        = onRedo
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
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
        //
        // The outer Box owns layout (its height tracks the unscaled content) and
        // clips the zoomed view. The inner Box carries the pinch-to-zoom detector
        // and the single graphicsLayer that scales/pans every layer together —
        // PDF background, sheet boxes and ink all live inside it, so a pinch zooms
        // them as one piece. pinchToZoom sits *outside* graphicsLayer so it reads
        // gestures in untransformed space, while the layers below receive
        // correctly transformed pointer coordinates (ink stays aligned at any zoom).
        Box(modifier = Modifier.fillMaxWidth().clipToBounds()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { zoomBoxSize = it }
                .pinchToZoom(
                    scaleProvider  = { zoomScale },
                    offsetProvider = { zoomOffset },
                    sizeProvider   = { zoomBoxSize },
                    onChange       = { s, o -> zoomScale = s; zoomOffset = o }
                )
                .graphicsLayer {
                    scaleX = zoomScale
                    scaleY = zoomScale
                    translationX = zoomOffset.x
                    translationY = zoomOffset.y
                    transformOrigin = TransformOrigin(0f, 0f)
                }
        ) {
            character.pdfPath?.let { path ->
                PdfBackground(pdfPath = path, modifier = Modifier.fillMaxWidth())
            }

        SheetCanvas(
            positions = workingPositions,
            rows = defaultRows(character.ruleset),
            modifier = Modifier.fillMaxWidth(),
            referenceWidthDp = workingReferenceWidth
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
                    temp = character.temporaryHp,
                    onEdit = if (editing) ({ onOpenDialog(ActiveDialog.EditHp) }) else null,
                    onDamage = { onOpenDialog(ActiveDialog.AdjustHp(isHeal = false)) },
                    onHeal = { onOpenDialog(ActiveDialog.AdjustHp(isHeal = true)) },
                    onTempHp = { onOpenDialog(ActiveDialog.AddTempHp) }
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

            SpellcastingBoxes(character, editing, onMove, onResize, onZChange, onFontScale, fontScaleFor, onCommit)

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
            // Ink canvas overlays the entire sheet area. matchParentSize() lets
            // SheetCanvas determine the Box height while InkCanvas fills it exactly.
            InkCanvas(
                strokes          = workingStrokes,
                inkMode          = inkMode,
                activeTool       = activeTool,
                penColor         = penColor,
                penWidthDp       = penWidth,
                clipboardStrokes = clipboardStrokes,
                onStrokeComplete = onStrokeComplete,
                onErase          = onErase,
                onSelectionMove  = onSelectionMove,
                onDelete         = onSelectionDelete,
                onCopy           = onCopy,
                onCut            = onCut,
                onPaste          = onPaste,
                onToolChange     = { activeTool = it },
                modifier         = Modifier.matchParentSize(),
                referenceWidthDp = workingReferenceWidth
            )
        } // end inner zoom Box (graphicsLayer)
        } // end PDF+canvas Box (clip + layout)
        } // end scrollable Column
    } // end outer Column
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
    temp: Int,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
    onDamage: () -> Unit,
    onHeal: () -> Unit,
    onTempHp: () -> Unit
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
            // Temporary HP lives in the same box; shown only when present so
            // the box stays compact when there's none.
            if (temp > 0) {
                Text(
                    "+$temp temp",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            // Damage / Temp HP / Heal sit on one row so the box stays usable
            // at small sizes — the temp-HP button shrinks to "T.Hp" to fit.
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
                    onClick = onTempHp,
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp)
                ) { Text("T.Hp", style = MaterialTheme.typography.labelMedium, maxLines = 1) }
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

// ── Spellcasting box IDs for 5.5e (per-class) ─────────────────────────────────
private val spellcastClassBoxIds = listOf(
    BoxId.SPELLCAST_CLASS_1, BoxId.SPELLCAST_CLASS_2, BoxId.SPELLCAST_CLASS_3,
    BoxId.SPELLCAST_CLASS_4, BoxId.SPELLCAST_CLASS_5, BoxId.SPELLCAST_CLASS_6
)

/**
 * Emits spellcasting box children for the SheetCanvas — nothing is emitted
 * when the character has no spellcasting classes, so the canvas rows collapse.
 *
 * **5.5e (2024)**: one box per spellcasting class showing ability, modifier,
 * save DC, and attack bonus for that class specifically.
 *
 * **5e (2014)**: three boxes — Spellcasting Ability, Spell Save DC, and
 * Spell Attack Bonus — each listing all caster classes.
 */
@Composable
private fun SpellcastingBoxes(
    character: Character,
    editing: Boolean,
    onMove: (BoxId, Float, Float) -> Unit,
    onResize: (BoxId, Float?, Float?) -> Unit,
    onZChange: (BoxId, Int) -> Unit,
    onFontScale: (BoxId, Int) -> Unit,
    fontScaleFor: (BoxId) -> Float,
    onCommit: () -> Unit
) {
    val rows = SpellcastingCalculator.rows(character)
    if (rows.isEmpty()) return

    if (character.ruleset == Ruleset.DND_5E_2024) {
        // One box per spellcasting class, up to 6.
        rows.take(6).forEachIndexed { index, row ->
            val boxId   = spellcastClassBoxIds[index]
            val abilMod = AbilityCalculator.modifier(character, row.ability)
            EditableSheetBox(boxId, editing, onMove, onResize, onZChange, onFontScale, fontScaleFor(boxId), onCommit) {
                SpellcastClassBox(row = row, abilityMod = abilMod)
            }
        }
    } else {
        // Three summary boxes — Ability | DC | Attack.
        val multiclass = rows.size > 1
        EditableSheetBox(BoxId.SPELLCAST_ABILITY, editing, onMove, onResize, onZChange, onFontScale, fontScaleFor(BoxId.SPELLCAST_ABILITY), onCommit) {
            SpellcastSummaryBox(
                title = "Spellcasting\nAbility",
                lines = rows.map { row ->
                    val mod = AbilityCalculator.modifier(character, row.ability)
                    if (multiclass) "${row.className}: ${row.ability.abbr} ${formatModifier(mod)}"
                    else "${row.ability.abbr}  ${formatModifier(mod)}"
                }
            )
        }
        EditableSheetBox(BoxId.SPELLCAST_DC, editing, onMove, onResize, onZChange, onFontScale, fontScaleFor(BoxId.SPELLCAST_DC), onCommit) {
            SpellcastSummaryBox(
                title = "Spell\nSave DC",
                lines = rows.map { row ->
                    if (multiclass) "${row.className}: ${row.saveDc}" else "${row.saveDc}"
                }
            )
        }
        EditableSheetBox(BoxId.SPELLCAST_ATTACK, editing, onMove, onResize, onZChange, onFontScale, fontScaleFor(BoxId.SPELLCAST_ATTACK), onCommit) {
            SpellcastSummaryBox(
                title = "Spell Attack\nBonus",
                lines = rows.map { row ->
                    if (multiclass) "${row.className}: ${formatModifier(row.attackBonus)}"
                    else formatModifier(row.attackBonus)
                }
            )
        }
    }
}

/**
 * 5.5e per-class spellcasting box. Matches image 1 reference layout:
 * title = class name + ability abbreviation; three labelled rows below for
 * the ability modifier, save DC (no sign), and attack bonus (signed).
 *
 * The "spellcasting modifier" on official sheets is the raw ability modifier,
 * not ability + PB. The attack bonus (ability + PB) is shown separately.
 */
@Composable
private fun SpellcastClassBox(row: SpellcastingRow, abilityMod: Int) {
    val scale = LocalBoxFontScale.current
    SheetBox(title = "${row.className}  •  ${row.ability.abbr}") {
        SpellStatLine(label = "Spellcasting Modifier", value = formatModifier(abilityMod),   scale = scale)
        SpellStatLine(label = "Spell Save DC",         value = row.saveDc.toString(),        scale = scale)
        SpellStatLine(label = "Spell Attack Bonus",    value = formatModifier(row.attackBonus), scale = scale)
    }
}

/** Single labelled stat line used inside spellcasting boxes. */
@Composable
private fun SpellStatLine(label: String, value: String, scale: Float) {
    val vertPad = (4.dp * scale).coerceAtLeast(2.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = vertPad, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text     = label,
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            modifier = Modifier.weight(1f)
        )
        Text(
            text       = value,
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * 5e per-category box (Ability | DC | Attack). One text line per caster class;
 * when only one class is present the class name prefix is omitted.
 */
@Composable
private fun SpellcastSummaryBox(title: String, lines: List<String>) {
    val scale = LocalBoxFontScale.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text      = title,
                style     = MaterialTheme.typography.labelSmall,
                color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height((4.dp * scale).coerceAtLeast(2.dp)))
            lines.forEach { line ->
                Text(
                    text       = line,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign  = androidx.compose.ui.text.style.TextAlign.Center
                )
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
    val overrideFor: (Skill) -> Boolean = { skill -> when (skill) {
        Skill.PERCEPTION    -> character.overrides.passivePerception    != null
        Skill.INVESTIGATION -> character.overrides.passiveInvestigation != null
        Skill.INSIGHT       -> character.overrides.passiveInsight       != null
        else                -> false
    }}

    if (editing) {
        val (sel, unsel) = Skill.entries.partition { it in selectedSet }
        val sorted = sel.sortedBy { it.display } + unsel.sortedBy { it.display }
        Column {
            sorted.forEach { skill ->
                PassiveToggleRow(
                    skill        = skill,
                    passiveScore = PassiveCalculator.passive(character, skill),
                    isSelected   = skill in selectedSet,
                    isOverridden = overrideFor(skill),
                    onClick      = { onToggle(skill) }
                )
            }
        }
    } else {
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
    isOverridden: Boolean,
    onClick: () -> Unit
) {
    val scale      = LocalBoxFontScale.current
    // Match StatRow geometry exactly so the dot, label and number sit in the
    // same place whether or not edit mode is active.
    val slotSize   = 16.dp * scale
    val dotSize    = 12.dp * scale
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
            text       = formatModifier(passiveScore),
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color      = onSurface.copy(alpha = contentAlpha)
        )
        // Trailing spacer/indicator matching StatRow so the number column lines
        // up between edit and view modes.
        PinnedIndicator(
            visible  = isOverridden && isSelected,
            modifier = Modifier.padding(start = 8.dp)
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
            // Compact clickable label rather than a TextButton: a TextButton
            // enforces a ~40dp minimum height that is taller than the title
            // text, so showing/hiding it would grow the header row and shove
            // the die rows down (very visible near the box's minimum size).
            // A plain clickable Text is no taller than the title, so the
            // header height — and everything below it — stays put.
            if (anySpent) {
                Text(
                    text = "Reset",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(onClick = onRestoreAll)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
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
