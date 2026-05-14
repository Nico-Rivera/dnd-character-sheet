package com.dndsheet.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Single-table Room database. Schema version starts at 1; every breaking
 * change to [CharacterEntity] needs a Migration registered here.
 *
 * For now the only column that could change without a migration is [json]
 * — kotlinx.serialization tolerates added optional fields, so domain-model
 * additions don't force a schema bump. The schema bumps when a *queryable*
 * column changes (anything other than `json`).
 */
@Database(
    entities = [CharacterEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao

    companion object {
        const val DATABASE_NAME = "dnd_character_sheet.db"

        @Volatile
        private var instance: AppDatabase? = null

        /**
         * Singleton accessor. The app injects this once at startup; production
         * code shouldn't be calling this from random places, but having a
         * convenience constructor keeps the wiring simple while there's no DI
         * framework yet.
         */
        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }

        private fun build(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            ).build()
    }
}
