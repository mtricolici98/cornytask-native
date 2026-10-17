package com.nobadhabbits.cornytask.features.notes

import android.text.Html
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nobadhabbits.cornytask.data.Note

/**
 * Notes grid. Long-press a note to start selecting, tap to add/remove more,
 * then delete them all at once from the top bar. Back leaves selection mode.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotesScreen(
    navController: NavController,
    viewModel: NotesViewModel = viewModel()
) {
    val notes by viewModel.notes.collectAsState()
    var selectedIds by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val selecting = selectedIds.isNotEmpty()

    // Drop ids of notes that no longer exist (e.g. deleted on another device).
    // Skip the initial empty list so a restored selection survives rotation.
    LaunchedEffect(notes) {
        if (notes.isNotEmpty()) {
            selectedIds = selectedIds intersect notes.map { it.id }.toSet()
        }
    }

    BackHandler(enabled = selecting) { selectedIds = emptySet() }

    if (confirmDelete) {
        val count = selectedIds.size
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(if (count == 1) "Delete note" else "Delete $count notes") },
            text = {
                Text(
                    if (count == 1) "Are you sure you want to delete this note?"
                    else "Are you sure you want to delete these $count notes?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteNotes(selectedIds)
                    selectedIds = emptySet()
                    confirmDelete = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            if (selecting) {
                TopAppBar(
                    title = { Text("${selectedIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { selectedIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = { selectedIds = notes.map { it.id }.toSet() }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select all")
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete selected")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                )
            }
        },
        floatingActionButton = {
            if (!selecting) {
                FloatingActionButton(onClick = {
                    navController.navigate("edit_note")
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Note")
                }
            }
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 128.dp),
            modifier = Modifier.padding(padding)
        ) {
            items(notes, key = { it.id }) { note ->
                val selected = note.id in selectedIds
                NoteGridItem(
                    note = note,
                    selected = selected,
                    onClick = {
                        if (selecting) {
                            selectedIds = if (selected) selectedIds - note.id else selectedIds + note.id
                        } else {
                            navController.navigate("More/edit_note/${note.id}")
                        }
                    },
                    onLongClick = {
                        selectedIds = selectedIds + note.id
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteGridItem(
    note: Note,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = if (selected) CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) else CardDefaults.cardColors()
    ) {
        Box {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = note.content.fromHtml(),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (selected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                )
            }
        }
    }
}

fun String.fromHtml(): AnnotatedString {
    return buildAnnotatedString {
        append(Html.fromHtml(this@fromHtml, Html.FROM_HTML_MODE_LEGACY))
    }
}
