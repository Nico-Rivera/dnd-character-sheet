package com.dndsheet.domain.repository

import com.dndsheet.domain.model.Character
import kotlinx.coroutines.flow.Flow

/**
 * Domain-level contract for character persistence. The implementation lives in
 * a later commit (:core:data, backed by Room) but the interface lives here so
 * the rules and UI modules can depend on it without pulling Android in.
 *
 * The observe* methods return [Flow] so the UI updates reactively when a save
 * happens elsewhere (autosave from another screen, an import, etc.).
 */
interface CharacterRepository {
    suspend fun create(character: Character): Character
    suspend fun update(character: Character): Character
    suspend fun delete(id: String)
    suspend fun get(id: String): Character?
    suspend fun list(): List<Character>

    fun observe(id: String): Flow<Character?>
    fun observeAll(): Flow<List<Character>>

    /** Returns the new character's id. */
    suspend fun duplicate(id: String, newName: String): String

    /** Round-trip JSON for export/import (see spec §6). */
    suspend fun exportJson(id: String): String
    suspend fun importJson(json: String): Character
}
