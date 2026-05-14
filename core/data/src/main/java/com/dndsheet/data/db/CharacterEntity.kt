package com.dndsheet.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Storage shape for a character. Hybrid schema:
 *
 *   - Top-level columns (id, name, ruleset, totalLevel, updatedAt, revision)
 *     are what the character-list screen needs. Listing characters can scan
 *     this table without parsing any JSON.
 *
 *   - [json] holds the full serialized [com.dndsheet.domain.model.Character].
 *     The domain model has nested collections (classes, weapons, spells,
 *     inventory, proficiencies) plus a free-form ManualOverrides map; trying
 *     to normalize all of that into Room tables would generate a lot of
 *     boilerplate without enabling any query the app actually needs.
 *
 * If a future feature wants to query "all characters with a Wizard level",
 * we'll add a derived column rather than denormalizing the whole thing.
 *
 * The mapper writes top-level columns and the JSON in one go, so they can't
 * drift out of sync within a single save.
 */
@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "ruleset")
    val ruleset: String,

    @ColumnInfo(name = "total_level")
    val totalLevel: Int,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    @ColumnInfo(name = "revision")
    val revision: Long,

    /** Full Character serialized as JSON via kotlinx.serialization. */
    @ColumnInfo(name = "json")
    val json: String
)
