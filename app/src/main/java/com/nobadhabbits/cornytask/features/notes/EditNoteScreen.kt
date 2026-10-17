package com.nobadhabbits.cornytask.features.notes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.nobadhabbits.cornytask.data.Note
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditNoteScreen(
    navController: NavController,
    noteId: String?,
    viewModel: NotesViewModel = viewModel()
) {
    // Saveable so a new note that was already autosaved (and got an id) isn't
    // saved again as a duplicate after a configuration change.
    var currentNoteId by rememberSaveable { mutableStateOf(noteId) }
    var title by remember { mutableStateOf("") }
    val state = rememberRichTextState()
    var note by remember { mutableStateOf<Note?>(null) }
    // (title, html) as last stored. Null while an existing note is still loading,
    // so leaving early can never overwrite it with the empty editor.
    var lastSaved by remember { mutableStateOf<Pair<String, String>?>(null) }

    LaunchedEffect(Unit) {
        val id = currentNoteId
        if (id == null) {
            lastSaved = "" to state.toHtml()
            return@LaunchedEffect
        }
        val loaded = viewModel.getNote(id) ?: return@LaunchedEffect
        note = loaded
        title = loaded.title
        state.setHtml(loaded.content)
        lastSaved = loaded.title to state.toHtml()
    }

    fun autosave() {
        val saved = lastSaved ?: return
        val html = state.toHtml()
        if (title to html == saved) return
        // Don't create empty notes
        if (note == null && title.isBlank() && state.annotatedString.text.isBlank()) return

        val toSave = note?.copy(title = title, content = html) ?: Note(title = title, content = html)
        viewModel.saveNote(toSave)?.let {
            note = it
            currentNoteId = it.id
            lastSaved = title to html
        }
    }

    // Autosave instead of a save button. ON_STOP fires when the app is hidden or closed
    // and when navigating away; onDispose covers back press / leaving the screen.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) autosave()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            autosave()
        }
    }

    // Key: use animation *target* IME insets, and exclude nav bars
    val imeNoNav = WindowInsets.imeAnimationTarget
        .exclude(WindowInsets.navigationBars)
        .only(WindowInsetsSides.Bottom)
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(innerPadding)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth().padding(1.dp)
            )

            RichTextStyleToolbar(state = state)

            RichTextEditor(
                state = state,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(1.dp)
                    .windowInsetsPadding(imeNoNav),

            )
        }
    }
}



@Composable
fun RichTextStyleToolbar(state: RichTextState) {
    Row {
        IconButton(onClick = {
            state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        }) {
            Icon(
                Icons.Default.FormatBold,
                contentDescription = "Bold",
                tint = if (state.currentSpanStyle.fontWeight == FontWeight.Bold)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(onClick = {
            state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))
        }) {
            Icon(
                Icons.Default.FormatItalic,
                contentDescription = "Italic",
                tint = if (state.currentSpanStyle.fontStyle == FontStyle.Italic)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(onClick = {
            state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
        }) {
            Icon(
                Icons.Default.FormatUnderlined,
                contentDescription = "Underline",
                tint = if (state.currentSpanStyle.textDecoration?.contains(TextDecoration.Underline) == true)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(onClick = { state.toggleUnorderedList() }) {
            Icon(
                Icons.Default.FormatListBulleted,
                contentDescription = "Unordered List",
                tint = if (state.isUnorderedList) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(onClick = { state.toggleOrderedList() }) {
            Icon(
                Icons.Default.FormatListNumbered,
                contentDescription = "Ordered List",
                tint = if (state.isOrderedList) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
