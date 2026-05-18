package com.dndsheet.app.nav

/**
 * String routes for navigation-compose. Kept as constants (not sealed
 * classes) because there are only two routes and one argument; the
 * extra ceremony of typed routes would dwarf the actual routing logic.
 */
object Routes {
    const val LIST = "characters"

    private const val OVERVIEW_BASE = "character"
    const val OVERVIEW_ARG_ID = "id"
    const val OVERVIEW_PATTERN = "$OVERVIEW_BASE/{$OVERVIEW_ARG_ID}"

    fun overview(id: String): String = "$OVERVIEW_BASE/$id"
}
