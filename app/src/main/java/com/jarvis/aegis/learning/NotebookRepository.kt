package com.jarvis.aegis.learning

import android.content.Context
import com.jarvis.aegis.security.SecurePreferences
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.UUID

class NotebookRepository(context: Context) {
    private val secure = SecurePreferences(context.getSharedPreferences("aegis_learning_v1", Context.MODE_PRIVATE))

    fun all(): List<Notebook> = runCatching {
        val array = JSONArray(secure.getString(NOTEBOOKS) ?: "[]")
        buildList {
            for (index in 0 until array.length()) add(array.getJSONObject(index).toNotebook())
        }.sortedByDescending(Notebook::updatedAt)
    }.getOrElse { emptyList() }

    fun save(notebook: Notebook) {
        val current = all().associateBy(Notebook::id).toMutableMap()
        current[notebook.id] = notebook.copy(updatedAt = Instant.now())
        persist(current.values.toList())
    }

    fun delete(id: UUID) = persist(all().filterNot { it.id == id })

    private fun persist(notebooks: List<Notebook>) {
        val array = JSONArray()
        notebooks.forEach { notebook ->
            array.put(JSONObject().apply {
                put("id", notebook.id.toString())
                put("title", notebook.title)
                put("subject", notebook.subject)
                put("content", notebook.content)
                put("sourceName", notebook.sourceName ?: "")
                put("createdAt", notebook.createdAt.toString())
                put("updatedAt", notebook.updatedAt.toString())
            })
        }
        secure.putString(NOTEBOOKS, array.toString())
    }

    private fun JSONObject.toNotebook() = Notebook(
        id = UUID.fromString(getString("id")),
        title = getString("title"),
        subject = optString("subject", "Custom"),
        content = getString("content"),
        sourceName = optString("sourceName").takeIf(String::isNotBlank),
        createdAt = Instant.parse(getString("createdAt")),
        updatedAt = Instant.parse(getString("updatedAt")),
    )

    companion object { private const val NOTEBOOKS = "notebooks" }
}
