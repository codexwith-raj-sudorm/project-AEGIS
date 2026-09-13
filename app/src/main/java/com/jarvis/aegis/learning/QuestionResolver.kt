package com.jarvis.aegis.learning

data class QuestionReference(val chapter: String?, val questionNumber: String)
data class ResolvedQuestion(val text: String, val page: Int?, val section: Int)

class QuestionResolver {
    fun parse(request: String): QuestionReference? {
        val question = QUESTION_REQUEST.find(request)?.groupValues?.get(1) ?: return null
        val chapter = CHAPTER_REQUEST.find(request)?.groupValues?.get(1)
        return QuestionReference(chapter, question)
    }

    fun resolve(notebook: Notebook, reference: QuestionReference): List<ResolvedQuestion> {
        var currentPage: Int? = null
        var currentChapter: String? = null
        val matches = mutableListOf<ResolvedQuestion>()
        val lines = notebook.content.lines()
        lines.forEachIndexed { index, raw ->
            PAGE.matchEntire(raw.trim())?.groupValues?.get(1)?.toIntOrNull()?.let { currentPage = it }
            CHAPTER.find(raw)?.groupValues?.get(1)?.let { currentChapter = it }
            val number = QUESTION_LINE.find(raw)?.groupValues?.get(1)
            if (number == reference.questionNumber && (reference.chapter == null || currentChapter == reference.chapter)) {
                val block = mutableListOf<String>()
                for (position in index until minOf(lines.size, index + 8)) {
                    if (position > index && QUESTION_LINE.find(lines[position]) != null) break
                    block += lines[position]
                }
                matches += ResolvedQuestion(block.joinToString("\n").trim(), currentPage, index + 1)
            }
        }
        return matches
    }

    private companion object {
        val QUESTION_REQUEST = Regex("(?:question|q(?:uestion)?\\.?)[ #:]*(\\d+[a-z]?)", RegexOption.IGNORE_CASE)
        val CHAPTER_REQUEST = Regex("chapter[ #:]*(\\d+[a-z]?)", RegexOption.IGNORE_CASE)
        val QUESTION_LINE = Regex("^\\s*(?:question|q)?\\s*(\\d+[a-z]?)[.):\\-]\\s*", RegexOption.IGNORE_CASE)
        val CHAPTER = Regex("chapter\\s+(\\d+[a-z]?)", RegexOption.IGNORE_CASE)
        val PAGE = Regex("\\[PAGE (\\d+)]")
    }
}
