package com.jarvis.aegis.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jarvis.aegis.learning.DocumentImporter
import com.jarvis.aegis.learning.LocalStudyRetriever
import com.jarvis.aegis.learning.Notebook
import com.jarvis.aegis.learning.NotebookRepository
import com.jarvis.aegis.learning.StudyMaterialGenerator
import com.jarvis.aegis.ui.components.AegisButton
import com.jarvis.aegis.ui.components.AegisOutlineButton
import kotlinx.coroutines.launch

@Composable
fun LearningScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { NotebookRepository(context) }
    var notebooks by remember { mutableStateOf(repository.all()) }
    var selected by remember { mutableStateOf<Notebook?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching { DocumentImporter(context).import(uri) }
                .onSuccess { document ->
                    selected = Notebook(
                        title = document.name.substringBeforeLast('.'), subject = "Custom",
                        content = document.content, sourceName = document.name,
                    )
                    document.warning?.let { importError = it }
                }
                .onFailure { importError = it.message ?: "Import failed" }
        }
    }

    if (selected == null) {
        NotebookListScreen(
            notebooks = notebooks,
            onCreate = { selected = Notebook(title = "", subject = "Custom", content = "") },
            onImport = { importer.launch(arrayOf("application/pdf", "image/*", "text/plain", "text/markdown", "text/*")) },
            onSelect = { selected = it },
            onDelete = { repository.delete(it.id); notebooks = repository.all() },
            onBack = onBack,
        )
    } else {
        NotebookEditorScreen(
            initial = selected!!,
            onSave = { repository.save(it); notebooks = repository.all(); selected = null },
            onCancel = { selected = null },
        )
    }

    importError?.let { message ->
        AlertDialog(
            onDismissRequest = { importError = null },
            title = { Text("IMPORT REJECTED") }, text = { Text(message) },
            confirmButton = { TextButton(onClick = { importError = null }) { Text("ACKNOWLEDGE") } },
        )
    }
}

@Composable
private fun NotebookListScreen(
    notebooks: List<Notebook>, onCreate: () -> Unit, onImport: () -> Unit,
    onSelect: (Notebook) -> Unit, onDelete: (Notebook) -> Unit, onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("> LEARNING_MODE // ENCRYPTED NOTEBOOKS")
        AegisButton("[ CREATE NOTEBOOK ]", onCreate, accent = true)
        AegisButton("[ IMPORT PDF, IMAGE, TEXT, OR MARKDOWN ]", onImport)
        if (notebooks.isEmpty()) Text("NO NOTEBOOKS. IMPORT OR CREATE STUDY MATERIAL.")
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(notebooks, key = { it.id }) { notebook ->
                Column(Modifier.fillMaxWidth()) {
                    Text("${notebook.title} // ${notebook.subject}")
                    Text("REV ${notebook.revision} // ${notebook.content.length} CHARACTERS // ${notebook.updatedAt}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { onSelect(notebook) }) { Text("OPEN") }
                        TextButton(onClick = { onDelete(notebook) }) { Text("DELETE") }
                    }
                }
            }
        }
        AegisOutlineButton("[ RETURN ]", onBack)
    }
}

@Composable
private fun NotebookEditorScreen(initial: Notebook, onSave: (Notebook) -> Unit, onCancel: () -> Unit) {
    var notebook by remember(initial.id) { mutableStateOf(initial) }
    var request by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("ASK A GROUNDED QUESTION OR GENERATE STUDY MATERIAL.") }
    val assistant = remember { LocalStudyRetriever() }
    val generator = remember { StudyMaterialGenerator() }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("> NOTEBOOK_EDITOR")
        OutlinedTextField(notebook.title, { notebook = notebook.copy(title = it) }, label = { Text("TITLE") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(notebook.subject, { notebook = notebook.copy(subject = it) }, label = { Text("SUBJECT") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(notebook.content, { notebook = notebook.copy(content = it) }, label = { Text("NOTEBOOK CONTENT") }, modifier = Modifier.fillMaxWidth().weight(1f))
        OutlinedTextField(request, { request = it }, label = { Text("ASK FROM THIS NOTEBOOK") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TextButton(onClick = {
                scope.launch {
                    val result = assistant.answer(notebook, request)
                    output = result.answer + if (result.citations.isEmpty()) "" else "\n\nSOURCES:\n${result.citations.joinToString("\n")}"
                }
            }, enabled = request.isNotBlank() && notebook.content.isNotBlank()) { Text("SEARCH") }
            TextButton(onClick = { output = generator.revisionNotes(notebook) }) { Text("NOTES") }
            TextButton(onClick = {
                output = generator.flashcards(notebook).joinToString("\n\n") { "Q: ${it.front}\nA: ${it.back}" }.ifBlank { "NO DEFINITION-STYLE LINES FOUND." }
            }) { Text("CARDS") }
            TextButton(onClick = {
                output = generator.quiz(notebook).joinToString("\n") { it.prompt }.ifBlank { "NOT ENOUGH STRUCTURED CONTENT FOR A QUIZ." }
            }) { Text("QUIZ") }
        }
        Text(output, modifier = Modifier.weight(0.6f))
        AegisButton("[ SAVE NOTEBOOK ]", { onSave(notebook) }, accent = true, enabled = notebook.title.isNotBlank() && notebook.content.isNotBlank())
        AegisOutlineButton("[ CANCEL ]", onCancel)
    }
}
