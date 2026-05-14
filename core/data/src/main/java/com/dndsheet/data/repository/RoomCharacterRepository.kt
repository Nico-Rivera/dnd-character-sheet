package com.dndsheet.data.repository

import com.dndsheet.data.db.CharacterDao
import com.dndsheet.data.mapper.CharacterMapper
import com.dndsheet.data.mapper.charJson
import com.dndsheet.domain.model.Character
import com.dndsheet.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Room-backed implementation of [CharacterRepository].
 *
 * Save semantics:
 *   - Every [create]/[update] bumps `revision` (monotonic) and `updatedAt`
 *     (wall clock). The UI can use `revision` to detect stale ViewModels;
 *     `updatedAt` drives the "recently edited" sort on the character list.
 *   - The bump happens here, not in [Character], so callers don't have to
 *     remember to do it. Tests can pass a fixed `now` to assert ordering.
 *
 * Export/import:
 *   - Export uses a "pretty" Json instance (indent + readable) since the
 *     output is intended for humans to read or for sharing/backup. Storage
 *     uses the compact Json from CharacterMapper.
 *   - Import does NOT preserve the imported character's id — it always
 *     assigns a fresh UUID. Importing the same file twice creates two
 *     characters, which matches user intent for backup-and-restore.
 *
 * @param now injectable for tests so timestamps are deterministic.
 */
class RoomCharacterRepository(
    private val dao: CharacterDao,
    private val now: () -> Long = { System.currentTimeMillis() }
) : CharacterRepository {

    private val prettyJson: Json = Json {
        prettyPrint = true
        encodeDefaults = false
        ignoreUnknownKeys = true
    }

    override suspend fun create(character: Character): Character {
        val stamped = character.copy(
            // If the caller already gave the character an id (e.g. a default
            // UUID from the data class init), keep it; otherwise mint one.
            id = character.id.ifBlank { UUID.randomUUID().toString() },
            revision = 0,
            createdAt = now(),
            updatedAt = now()
        )
        dao.insert(CharacterMapper.toEntity(stamped))
        return stamped
    }

    override suspend fun update(character: Character): Character {
        val stamped = character.copy(
            revision = character.revision + 1,
            updatedAt = now()
        )
        dao.update(CharacterMapper.toEntity(stamped))
        return stamped
    }

    override suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    override suspend fun get(id: String): Character? =
        dao.get(id)?.let(CharacterMapper::toDomain)

    override suspend fun list(): List<Character> =
        dao.list().map(CharacterMapper::toDomain)

    override fun observe(id: String): Flow<Character?> =
        dao.observe(id).map { it?.let(CharacterMapper::toDomain) }

    override fun observeAll(): Flow<List<Character>> =
        dao.observeAll().map { rows -> rows.map(CharacterMapper::toDomain) }

    override suspend fun duplicate(id: String, newName: String): String {
        val source = get(id) ?: error("Character not found: $id")
        val copy = source.copy(
            id = UUID.randomUUID().toString(),
            name = newName,
            revision = 0,
            createdAt = now(),
            updatedAt = now()
        )
        dao.insert(CharacterMapper.toEntity(copy))
        return copy.id
    }

    override suspend fun exportJson(id: String): String {
        val character = get(id) ?: error("Character not found: $id")
        return prettyJson.encodeToString(Character.serializer(), character)
    }

    override suspend fun importJson(json: String): Character {
        val parsed = charJson.decodeFromString(Character.serializer(), json)
        // Always assign a fresh id on import so users can re-import the
        // same backup without overwriting anything.
        val withFreshId = parsed.copy(
            id = UUID.randomUUID().toString(),
            revision = 0,
            createdAt = now(),
            updatedAt = now()
        )
        dao.insert(CharacterMapper.toEntity(withFreshId))
        return withFreshId
    }
}
