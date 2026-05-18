package com.dndsheet.app

import android.content.Context
import com.dndsheet.data.db.AppDatabase
import com.dndsheet.data.repository.RoomCharacterRepository
import com.dndsheet.domain.repository.CharacterRepository

/**
 * Manual service locator. Will get replaced with Hilt later, but at this
 * scale a single container with two fields is much less ceremony than
 * setting up codegen for a project that has exactly one repository.
 *
 * Held by [DnDApplication]; reachable from a Composable via:
 *
 *     val container = (LocalContext.current.applicationContext as DnDApplication).container
 */
class AppContainer(context: Context) {
    private val database: AppDatabase = AppDatabase.getInstance(context)
    val characterRepository: CharacterRepository =
        RoomCharacterRepository(database.characterDao())
}
