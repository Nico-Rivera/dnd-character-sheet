package com.dndsheet.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * A single point captured during a pen stroke. Coordinates are in dp,
 * relative to the top-left of the ink canvas (which fills the sheet area).
 */
@Serializable
data class StrokePoint(val x: Float, val y: Float)

/**
 * One continuous stroke from pen-down to pen-up. Each stroke is its own
 * entity: the eraser removes strokes whole, and the selection tool operates
 * on sets of strokes.
 *
 * [color] is ARGB packed as a Long (use `Color.value` in Compose or
 * `toArgb().toLong()` when converting). Long avoids the UInt serialization
 * quirk present in some kotlinx.serialization versions.
 *
 * [width] is the stroke width in dp.
 */
@Serializable
data class Stroke(
    val id: String = UUID.randomUUID().toString(),
    val points: List<StrokePoint>,
    val color: Long,
    val width: Float
)
