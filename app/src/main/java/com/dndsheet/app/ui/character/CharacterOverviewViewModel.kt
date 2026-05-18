package com.dndsheet.app.ui.character

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dndsheet.domain.model.Character
import com.dndsheet.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs the read-only sheet view. Holds nothing but the observed character —
 * all derived values get computed in the screen via the calculators, which is
 * cheap and keeps the ViewModel free of domain logic.
 *
 * State is a small sealed wrapper so the screen can render Loading and
 * NotFound distinctly (rather than empty / placeholder content that lies
 * about whether the character exists).
 */
class CharacterOverviewViewModel(
    private val characterId: String,
    private val repository: CharacterRepository
) : ViewModel() {

    val state: StateFlow<OverviewState> = repository.observe(characterId)
        .map<Character?, OverviewState> { c ->
            if (c == null) OverviewState.NotFound else OverviewState.Loaded(c)
        }
        .onStart<OverviewState> { emit(OverviewState.Loading) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = OverviewState.Loading
        )

    /**
     * Apply a transformation to the loaded character and persist it.
     * The repository handles `revision` + `updatedAt` bumps.
     *
     * Domain-level validation (HP >= 0, totalLevel <= 20, etc.) lives in
     * the [Character] init blocks. If a transform produces an invalid
     * character we log and drop the change — the UI dialogs validate
     * up-front so this is a backstop, not the primary defense.
     */
    fun update(transform: (Character) -> Character) {
        val loaded = (state.value as? OverviewState.Loaded)?.character ?: return
        viewModelScope.launch {
            try {
                repository.update(transform(loaded))
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Rejected invalid update: ${e.message}")
            }
        }
    }

    companion object {
        const val ARG_CHARACTER_ID = "characterId"
        private const val TAG = "OverviewVM"

        fun factory(characterId: String, repository: CharacterRepository) = viewModelFactory {
            initializer {
                CharacterOverviewViewModel(characterId, repository)
            }
        }
    }
}

sealed interface OverviewState {
    data object Loading : OverviewState
    data object NotFound : OverviewState
    data class Loaded(val character: Character) : OverviewState
}
