package com.dndsheet.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for character storage.
 *
 * The observe* methods return [Flow] so the UI can react to changes from any
 * source — autosave, an import, a delete on another screen — without polling.
 *
 * `OnConflictStrategy.REPLACE` on insert is intentional: import flows can
 * legitimately overwrite an existing character with the same id (e.g. user
 * re-imports a character they already have); the repository checks for the
 * conflict beforehand if it wants to surface it to the user, but the DAO
 * itself just does what it's told.
 */
@Dao
interface CharacterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CharacterEntity)

    @Update
    suspend fun update(entity: CharacterEntity)

    @Delete
    suspend fun delete(entity: CharacterEntity)

    @Query("DELETE FROM characters WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM characters WHERE id = :id")
    suspend fun get(id: String): CharacterEntity?

    @Query("SELECT * FROM characters ORDER BY updated_at DESC")
    suspend fun list(): List<CharacterEntity>

    @Query("SELECT * FROM characters WHERE id = :id")
    fun observe(id: String): Flow<CharacterEntity?>

    @Query("SELECT * FROM characters ORDER BY updated_at DESC")
    fun observeAll(): Flow<List<CharacterEntity>>
}
