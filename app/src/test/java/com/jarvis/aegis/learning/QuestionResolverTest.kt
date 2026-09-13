package com.jarvis.aegis.learning

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionResolverTest {
    private val notebook = Notebook(
        title = "Physics",
        subject = "Physics",
        content = """
            [PAGE 10]
            Chapter 4 Motion
            12. Calculate velocity after five seconds.
            Use v = u + at.

            [PAGE 20]
            Chapter 5 Force
            12. Calculate force for the given mass.
            Use F = ma.
        """.trimIndent(),
    )

    @Test fun parsesChapterAndQuestionReference() {
        assertEquals(QuestionReference("4", "12"), QuestionResolver().parse("Give solution to question 12 from chapter 4"))
    }

    @Test fun resolvesQuestionToCorrectPage() {
        val result = QuestionResolver().resolve(notebook, QuestionReference("4", "12")).single()
        assertEquals(10, result.page)
        assertTrue("velocity" in result.text)
    }

    @Test fun asksForDisambiguationWhenQuestionRepeats() = runBlocking {
        val result = LocalStudyRetriever().answer(notebook, "Explain question 12")
        assertTrue("more than once" in result.answer)
        assertEquals(2, result.citations.size)
    }
}
