package com.dndsheet.domain.model

import kotlinx.serialization.Serializable

/**
 * Absolute position + size for a single sheet box, in dp. Stored per
 * character so each character can arrange its sheet (and, later, line boxes
 * up with an uploaded PDF template) independently.
 *
 * @param x dp from the left edge of the sheet canvas.
 * @param y dp from the top of the sheet canvas.
 * @param width  dp; 0 means "use the box's natural/default width".
 * @param height dp; 0 means "use the box's natural/default height".
 * @param z draw order — higher is drawn on top. Used for the
 *        bring-to-front / send-to-back controls when boxes overlap.
 *
 * Width/height of 0 (rather than null) keeps the JSON compact and lets a
 * box be moved without pinning its size: a dragged-but-never-resized box
 * keeps wrapping its content as the content changes.
 */
@Serializable
data class BoxPosition(
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f,
    val z: Int = 0,
    /** Per-box font scale. 1.0 = default, 0.5–2.0 range, steps of 0.1. */
    val fontScale: Float = 1f
)

/**
 * Per-character sheet layout: a map from box id (the stable name of the UI's
 * BoxId enum) to its [BoxPosition]. Boxes absent from the map fall back to
 * the app's default row-based layout, so an untouched character — and every
 * character created before this feature existed — renders exactly as before.
 *
 * The map is intentionally keyed by String rather than an enum so the domain
 * module stays free of UI concerns; the :app module owns the BoxId enum and
 * is responsible for using consistent key names.
 */
@Serializable
data class SheetLayout(
    val positions: Map<String, BoxPosition> = emptyMap()
)
