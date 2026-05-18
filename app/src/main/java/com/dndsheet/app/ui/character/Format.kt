package com.dndsheet.app.ui.character

/** "+3", "+0", "−1" — sheet-style signed modifier with U+2212 minus. */
fun formatModifier(value: Int): String = when {
    value > 0 -> "+$value"
    value < 0 -> "−${-value}"
    else -> "+0"
}
