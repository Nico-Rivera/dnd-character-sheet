package com.dndsheet.data.mapper

import com.dndsheet.data.db.CharacterEntity
import com.dndsheet.domain.model.Character
import kotlinx.serialization.json.Json

/**
 * Translates between the domain [Character] and its Room storage form.
 *
 * The JSON instance is `internal` and shared across the data layer — making
 * a fresh `Json {...}` per call would be measurable overhead during autosave.
 * `ignoreUnknownKeys = true` gives forward compatibility: a character saved
 * by a future build with a new field can be read by an older build without
 * crashing (the new field just gets dropped). `encodeDefaults = false`
 * keeps the JSON small by omitting fields that match their constructor
 * defaults — relevant because the domain has lots of "= emptyList()" fields.
 */
internal val charJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
}

object CharacterMapper {

    fun toEntity(character: Character): CharacterEntity = CharacterEntity(
        id = character.id,
        name = character.name,
        ruleset = character.ruleset.name,
        totalLevel = character.totalLevel,
        updatedAt = character.updatedAt,
        revision = character.revision,
        json = charJson.encodeToString(Character.serializer(), character)
    )

    fun toDomain(entity: CharacterEntity): Character =
        charJson.decodeFromString(Character.serializer(), entity.json)
}
