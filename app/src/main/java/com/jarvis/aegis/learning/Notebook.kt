package com.jarvis.aegis.learning

import java.time.Instant
import java.util.UUID

data class Notebook(
    val id: UUID = UUID.randomUUID(),
    val title: String,
    val subject: String,
    val content: String,
    val sourceName: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = createdAt,
)

data class StudyAnswer(
    val answer: String,
    val citations: List<String>,
    val grounded: Boolean,
)

interface LearningAssistant {
    suspend fun answer(notebook: Notebook, request: String): StudyAnswer
}

/** Retrieval-only baseline that never fabricates model output. */
class LocalStudyRetriever : LearningAssistant {
    override suspend fun answer(notebook: Notebook, request: String): StudyAnswer {
        val terms = request.lowercase().split(Regex("\\W+")).filter { it.length > 3 }.toSet()
        val sections = notebook.content.toSections()
        val matches = sections.mapIndexed { index, text ->
            Triple(index + 1, text, terms.count { it in text.lowercase() })
        }.filter { it.third > 0 }.sortedByDescending { it.third }.take(3)
        if (matches.isEmpty()) return StudyAnswer(
            answer = "The imported notebook does not contain enough matching information.",
            citations = emptyList(), grounded = false,
        )
        return StudyAnswer(
            answer = matches.joinToString("\n\n") { it.second.trim() },
            citations = matches.map { "${notebook.title}, section ${it.first}" },
            grounded = true,
        )
    }
}

internal fun String.toSections(): List<String> =
    split(Regex("\\n\\s*\\n|(?=^#{1,3}\\s)", RegexOption.MULTILINE))
        .map(String::trim).filter(String::isNotBlank)
