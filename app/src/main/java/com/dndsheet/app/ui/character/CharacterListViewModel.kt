package com.dndsheet.app.ui.character

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dndsheet.app.debug.SeedData
import com.dndsheet.domain.model.Character
import com.dndsheet.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CharacterListViewModel(
    private val repository: CharacterRepository
) : ViewModel() {

    /**
     * Hot StateFlow so configuration changes (rotation) don't lose the
     * list, and screen reentries don't refetch. The 5s stop timeout is
     * the standard recipe — keeps the upstream alive across short app
     * backgrounding.
     */
    val characters: StateFlow<List<Character>> = repository.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /** Creates a barebones character — Fighter 1, default abilities. The
     *  user fills in details once the editing UI lands (commit 4). */
    fun createBlank() {
        viewModelScope.launch {
            repository.create(Character(name = "New Character"))
        }
    }

    /** Inserts the canned example character so there's a non-trivial sheet
     *  to render on a fresh install. */
    fun createExample() {
        viewModelScope.launch {
            repository.create(SeedData.exampleWizardCleric())
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun duplicate(id: String, baseName: String) {
        viewModelScope.launch {
            repository.duplicate(id, "$baseName (copy)")
        }
    }

    companion object {
        fun factory(repository: CharacterRepository) = viewModelFactory {
            initializer { CharacterListViewModel(repository) }
        }
    }
}
