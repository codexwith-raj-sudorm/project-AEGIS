package com.jarvis.aegis.learning

data class Flashcard(val front: String, val back: String)
data class QuizQuestion(val prompt: String, val answer: String)

class StudyMaterialGenerator {
    fun revisionNotes(notebook: Notebook): String {
        val sections = notebook.content.toSections()
        return buildString {
            appendLine("# ${notebook.title} — Revision Notes")
            sections.take(12).forEachIndexed { index, section ->
                val lines = section.lines().map(String::trim).filter(String::isNotBlank)
                appendLine("\n## Section ${index + 1}")
                lines.take(4).forEach { appendLine("- $it") }
            }
        }.trim()
    }

    fun flashcards(notebook: Notebook, limit: Int = 12): List<Flashcard> =
        notebook.content.lines().mapNotNull { line ->
            val separator = when {
                ':' in line -> ':'
                '=' in line -> '='
                else -> null
            } ?: return@mapNotNull null
            val pieces = line.split(separator, limit = 2).map(String::trim)
            if (pieces.size == 2 && pieces.all { it.length in 2..300 }) Flashcard(pieces[0], pieces[1]) else null
        }.distinctBy(Flashcard::front).take(limit)

    fun quiz(notebook: Notebook, limit: Int = 10): List<QuizQuestion> =
        flashcards(notebook, limit).map { QuizQuestion("Define or explain: ${it.front}", it.back) }
}
