package com.jarvis.aegis.learning

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyMaterialGeneratorTest {
    private val notebook = Notebook(
        title = "Motion",
        subject = "Physics",
        content = "Velocity: displacement per unit time.\nAcceleration: change in velocity per unit time.\n\nMomentum equals mass times velocity.",
    )

    @Test fun createsFlashcardsFromDefinitions() {
        val cards = StudyMaterialGenerator().flashcards(notebook)
        assertTrue(cards.any { it.front == "Velocity" })
        assertTrue(cards.any { it.front == "Acceleration" })
    }

    @Test fun generatedQuizDoesNotEmbedAnswersInPrompts() {
        val quiz = StudyMaterialGenerator().quiz(notebook)
        assertTrue(quiz.isNotEmpty())
        assertTrue(quiz.none { it.answer in it.prompt })
    }

    @Test fun retrievalCitesMatchingSections() = runBlocking {
        val result = LocalStudyRetriever().answer(notebook, "Explain momentum and velocity")
        assertTrue(result.grounded)
        assertFalse(result.citations.isEmpty())
    }
}
