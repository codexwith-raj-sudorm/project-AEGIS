package com.jarvis.aegis.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarvis.aegis.learning.LocalStudyRetriever
import com.jarvis.aegis.learning.Notebook
import com.jarvis.aegis.ui.components.AegisButton
import com.jarvis.aegis.ui.components.AegisOutlineButton
import kotlinx.coroutines.launch

@Composable
fun LearningScreen(onBack: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var request by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("ADD NOTEBOOK TEXT AND ASK A QUESTION.") }
    val scope = rememberCoroutineScope()
    val assistant = remember { LocalStudyRetriever() }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("> LEARNING_MODE // LOCAL RETRIEVAL")
        OutlinedTextField(title, { title = it }, label = { Text("NOTEBOOK TITLE") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(content, { content = it }, label = { Text("PASTE NOTEBOOK / CHAPTER") }, modifier = Modifier.fillMaxWidth().weight(1f))
        OutlinedTextField(request, { request = it }, label = { Text("ASK FROM THIS MATERIAL") }, modifier = Modifier.fillMaxWidth())
        AegisButton("[ SEARCH NOTEBOOK ]", {
            scope.launch {
                val result = assistant.answer(Notebook(title = title.ifBlank { "Notebook" }, subject = "Custom", content = content), request)
                answer = result.answer + if (result.citations.isEmpty()) "" else "\n\nSOURCES: ${result.citations.joinToString()}"
            }
        }, accent = true, enabled = content.isNotBlank() && request.isNotBlank())
        Text(answer)
        AegisOutlineButton("[ RETURN ]", onBack)
    }
}
