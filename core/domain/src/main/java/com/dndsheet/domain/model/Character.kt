package com.dndsheet.domain.model

import com.dndsheet.domain.enums.Alignment
import com.dndsheet.domain.enums.Ruleset
import com.dndsheet.domain.enums.Skill
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * The aggregate root for a character sheet. Everything the calculation engine
 * needs to derive a character's stats is reachable from here.
 *
 * Fields that are spec'd but don't influence any calculation (conditions,
 * notes, status effects, alignment) live as plain strings/enums; the engine
 * is intentionally agnostic to them.
 */
@Serializable
data class Character(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val species: String = "",
    val background: String = "",
    val alignment: Alignment = Alignment.UNALIGNED,
    val ruleset: Ruleset = Ruleset.DND_5E_2024,

    val classes: List<ClassLevel> = emptyList(),
    val abilityScores: AbilityScores = AbilityScores(),
    val proficiencies: Proficiencies = Proficiencies(),

    val weapons: List<Weapon> = emptyList(),
    val spells: List<Spell> = emptyList(),
    val inventory: List<InventoryItem> = emptyList(),

    val maxHp: Int = 0,
    val currentHp: Int = 0,
    val temporaryHp: Int = 0,
    val hitDiceRemaining: Map<Int, Int> = emptyMap(),  // hit die size -> count remaining

    val experience: Int = 0,
    val inspiration: Boolean = false,

    val conditions: List<String> = emptyList(),
    val notes: String = "",

    val overrides: ManualOverrides = ManualOverrides(),

    /**
     * Skills the user has chosen to display in the Passives box. Persisted so
     * each character can show only the passives relevant to their build.
     * Defaults to the classic three that every table references most often.
     */
    val passiveSkills: List<Skill> = listOf(Skill.PERCEPTION, Skill.INVESTIGATION, Skill.INSIGHT),

    /** Per-character sheet box arrangement. Empty = default layout. */
    val layout: SheetLayout = SheetLayout(),

    /**
     * Absolute path inside the app's internal storage to a PDF used as the
     * sheet background. Null = no PDF (default blank canvas).
     *
     * Future: a companion pdfLayout field will carry CV-detected field
     * positions so boxes can be auto-aligned to the PDF's structure.
     */
    val pdfPath: String? = null,

    /** Monotonically increasing — bumped by the data layer on every save. */
    val revision: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /** Sum of all class levels. The single most-queried derived value. */
    val totalLevel: Int get() = classes.sumOf { it.level }

    init {
        require(currentHp >= 0) { "Current HP cannot be negative (use death saves separately)" }
        require(temporaryHp >= 0) { "Temporary HP cannot be negative" }
        require(experience >= 0) { "Experience cannot be negative" }
        require(maxHp >= 0) { "Max HP cannot be negative" }
        // Total level cap: 20 in both editions, even across multiclass.
        require(totalLevel in 0..20) { "Total character level out of range: $totalLevel" }
    }
}
