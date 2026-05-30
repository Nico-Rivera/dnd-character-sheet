# Ink Layer — Implementation Plan

Picking up after **Commit 8** (per-box font scale + configurable passives).

## Context / decisions made

- Ink layer sits **always on top** of all sheet boxes (single global canvas, not per-box).
- Ink mode is toggled via a button in the top app bar.
  - Ink mode and edit mode are **mutually exclusive**: entering one exits the other.
  - The ink toolbar (and its buttons) is **hidden** when not in ink mode.
- Strokes are **persisted with the character** (serialized into the Character JSON blob).
- Undo/redo history: **50 stroke snapshots** (ArrayDeque, capped).
- Eraser removes the **whole stroke** when any part of it is touched.
- Samsung Notes-style: each stroke is its own entity.

---

## Commit 9 — Ink data model

**Files to create / modify:**

### `core/domain/src/main/java/com/dndsheet/domain/model/Stroke.kt` (new)

```kotlin
package com.dndsheet.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class StrokePoint(val x: Float, val y: Float)

@Serializable
data class Stroke(
    val id: String = UUID.randomUUID().toString(),
    val points: List<StrokePoint>,
    val color: Long,   // ARGB packed as Long (use Color.value from Compose or toArgb().toLong())
    val width: Float   // stroke width in dp
)
```

### `core/domain/src/main/java/com/dndsheet/domain/model/Character.kt` (edit)

Add import and field:
```kotlin
import com.dndsheet.domain.model.Stroke   // already in same package, may not need import

// Inside the data class, after val layout:
val inkStrokes: List<Stroke> = emptyList(),
```

No Room migration needed — new field with default value, ignored by `ignoreUnknownKeys = true` on old builds.

---

## Commit 10 — Ink mode + pen tool + toolbar + undo/redo

### Architecture overview

- `InkCanvas` composable: fills the entire sheet area, sits above all boxes (`zIndex = Float.MAX_VALUE`). Only captures touch when ink mode is active; otherwise `pointerInput` is a no-op so touches pass through to boxes.
- `InkToolbar`: horizontal row of controls shown only in ink mode. Lives inside the existing scaffold body (not in the top app bar). Contains: Pen | Eraser | Selection tool buttons + color palette chips + width slider + Undo/Redo buttons.
- Ink mode state lives in `CharacterOverviewViewModel` (or a sibling `InkViewModel` if preferred).

### State to add to ViewModel

```kotlin
// current tool
enum class InkTool { PEN, ERASER, SELECTION }

var inkMode: Boolean = false         // drives toolbar visibility + canvas capture
var activeTool: InkTool = InkTool.PEN
var penColor: Long = 0xFF000000L     // ARGB
var penWidth: Float = 4f             // dp
var inkStrokes: List<Stroke>         // mirrors character.inkStrokes, saved on change

// undo/redo
val undoStack: ArrayDeque<List<Stroke>> = ArrayDeque()   // max 50
val redoStack: ArrayDeque<List<Stroke>> = ArrayDeque()
```

Entering ink mode: `inkMode = true`, if edit mode was active → exit edit mode.
Entering edit mode: if ink mode was active → `inkMode = false`.

### `InkCanvas` touch handling (pen tool)

```
ACTION_DOWN  → start new in-progress path, record first StrokePoint
ACTION_MOVE  → append StrokePoint to in-progress path, trigger recompose
ACTION_UP    → finalize: push current inkStrokes snapshot onto undoStack
               (cap at 50, drop oldest if needed), clear redoStack,
               append new Stroke to inkStrokes, clear in-progress path,
               persist to character via viewModel.update { ... }
```

Drawing: iterate committed strokes + in-progress stroke, draw each as a `Path` on an `androidx.compose.ui.graphics.Canvas` using `drawPath` with `Stroke` paint (cap = Round, join = Round).

### Color palette

Six preset chips (black, dark red, dark blue, dark green, purple, white/eraser-preview). Tapping one sets `penColor`. A custom color option can come later.

### Width slider

`Slider` ranging 1f..20f (dp), value = `penWidth`. Label shows current value.

### Undo/Redo

- **Undo**: pop from undoStack → push current inkStrokes onto redoStack → apply popped state → persist.
- **Redo**: pop from redoStack → push current onto undoStack → apply → persist.

---

## Commit 11 — Eraser tool

When `activeTool == ERASER`:
- On `ACTION_DOWN` and every `ACTION_MOVE` event, find all strokes where **any segment** of their point list comes within `eraserRadius` of the touch point.
- `eraserRadius = penWidth * 2f` (scales with selected width so a wider pen = wider eraser).
- Remove matched strokes from `inkStrokes`.
- Each gesture (DOWN → UP) is a **single undo step**: snapshot before the gesture starts, push to undoStack on UP.

Hit detection helper (pure function, can live in a `StrokeHitTester` object in `:core:rules` or locally in the UI):

```kotlin
fun hitsStroke(stroke: Stroke, x: Float, y: Float, radius: Float): Boolean {
    val points = stroke.points
    if (points.isEmpty()) return false
    if (points.size == 1) return dist(points[0], x, y) <= radius
    for (i in 0 until points.size - 1) {
        if (distToSegment(points[i], points[i+1], x, y) <= radius) return true
    }
    return false
}
```

Eraser cursor: draw a circle at current touch position sized to `eraserRadius` (outline only) so the user can see what they're about to erase.

---

## Commit 12a — Selection tool: move, copy, cut, delete

When `activeTool == SELECTION`:

### Rubber-band selection
- `ACTION_DOWN` on empty area → start drag rect.
- `ACTION_MOVE` → update rect, highlight strokes whose **bounding box** intersects the rect.
- `ACTION_UP` → commit selection (set of stroke IDs).

Bounding box of a stroke = `min/max of all StrokePoint x/y values`.

### Selected state visuals
- Draw a dashed rectangle around the union bounding box of all selected strokes.
- Selected strokes render in a slightly desaturated color so they read as "grabbed".

### Move
- `ACTION_DOWN` inside the selection bounding box (not on a handle) → start move gesture.
- `ACTION_MOVE` → translate all selected strokes: `point.copy(x = point.x + dx, y = point.y + dy)`.
- `ACTION_UP` → commit, push undo snapshot.

### Action bar
Appears below the selection bounding box when ≥1 stroke is selected:
- **Delete** — remove selected strokes, push undo.
- **Cut** — copy to clipboard (in-memory `List<Stroke>` with new IDs), delete, push undo.
- **Copy** — copy to clipboard only, no deletion, no undo step.
- **Paste** — insert clipboard strokes offset by (+16dp, +16dp) so they don't sit exactly on top, push undo.

Tapping outside the selection clears it (no undo step).

---

## Commit 12b — Selection tool: resize and rotate

Builds on 12a's selection state.

### Resize (corner handles)
Four corner handles on the selection bounding box. Dragging a corner:
- Compute scale factors `sx = newWidth / oldWidth`, `sy = newHeight / oldHeight`.
- For every selected stroke, transform each point:
  ```
  newX = selectionOriginX + (point.x - selectionOriginX) * sx
  newY = selectionOriginY + (point.y - selectionOriginY) * sy
  ```
- `selectionOrigin` = top-left corner of the union bounding box.
- Hold Shift (or single-axis drag) for non-uniform scale; default is free-form.
- On `ACTION_UP`: push undo snapshot.

### Rotate (top-center handle)
A circular handle centered above the selection bounding box.
- Dragging it computes the angle delta from the selection center.
- For every selected stroke, rotate each point around the selection center:
  ```
  dx = point.x - cx;  dy = point.y - cy
  newX = cx + dx*cos(θ) - dy*sin(θ)
  newY = cy + dx*sin(θ) + dy*cos(θ)
  ```
- Show a live angle readout (e.g. "34°") next to the handle while dragging.
- On `ACTION_UP`: push undo snapshot.

### Handle hit sizes
All handles should have a minimum touch target of 48×48 dp (Material guideline), even if the visual handle is smaller (12dp circle). Use a transparent hit-area overlay.

---

## Hard process rules (carry forward)

- All file changes via Write/Edit tools — never via bash.
- User runs git and Gradle themselves in Android Studio.
- Do NOT revert AGP 8.13.2, foojay plugin, `local.properties`, or `implementation`→`api` in `core/data`.
- bash cannot delete files in the mounted folder without `mcp__cowork__allow_cowork_file_delete`.
