package com.dndsheet.app.ui.character

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dndsheet.app.DnDApplication
import com.dndsheet.domain.model.Character
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterListScreen(
    onOpen: (String) -> Unit,
    viewModel: CharacterListViewModel = vmFromContainer()
) {
    val characters by viewModel.characters.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Characters") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.createBlank() }) {
                Icon(Icons.Default.Add, contentDescription = "New character")
            }
        }
    ) { padding ->
        if (characters.isEmpty()) {
            EmptyState(
                onSeed = { viewModel.createExample() },
                onCreate = { viewModel.createBlank() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            CharacterList(
                characters = characters,
                onOpen = onOpen,
                onDelete = { viewModel.delete(it) },
                onDuplicate = { id, name -> viewModel.duplicate(id, name) },
                contentPadding = padding
            )
        }
    }
}

@Composable
private fun CharacterList(
    characters: List<Character>,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDuplicate: (String, String) -> Unit,
    contentPadding: PaddingValues
) {
    LazyColumn(contentPadding = contentPadding, modifier = Modifier.fillMaxSize()) {
        items(items = characters, key = { it.id }) { character ->
            CharacterRow(
                character = character,
                onOpen = { onOpen(character.id) },
                onDelete = { onDelete(character.id) },
                onDuplicate = { onDuplicate(character.id, character.name.ifBlank { "Unnamed" }) }
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun CharacterRow(
    character: Character,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = character.name.ifBlank { "Unnamed" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = classLine(character),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Last edited ${formatDate(character.updatedAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Duplicate") },
                    onClick = { menuOpen = false; onDuplicate() }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = { menuOpen = false; onDelete() }
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    onSeed: () -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No characters yet",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Tap + to start a fresh sheet, or load the example below to see the calculation engine in action.",
            style = MaterialTheme.typography.bodyMedium
        )
        ExtendedFloatingActionButton(
            text = { Text("Add example character") },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            onClick = onSeed
        )
        ExtendedFloatingActionButton(
            text = { Text("New blank character") },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            onClick = onCreate
        )
    }
}

/** "Wizard 4 / Cleric 1" or "—" if no classes yet. */
private fun classLine(character: Character): String {
    if (character.classes.isEmpty()) return "—"
    return character.classes.joinToString(" / ") { "${it.className} ${it.level}" }
}

private fun formatDate(epochMillis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(Date(epochMillis))

/** Pulls the repository out of the Application-scoped container. */
@Composable
private fun vmFromContainer(): CharacterListViewModel {
    val container = (LocalContext.current.applicationContext as DnDApplication).container
    return viewModel(factory = CharacterListViewModel.factory(container.characterRepository))
}
