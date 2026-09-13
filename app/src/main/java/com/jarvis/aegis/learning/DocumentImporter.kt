package com.jarvis.aegis.learning

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

data class ImportedDocument(
    val name: String,
    val content: String,
    val pageCount: Int?,
    val mediaType: String,
)

class DocumentImporter(private val context: Context) {
    suspend fun import(uri: Uri): ImportedDocument = withContext(Dispatchers.IO) {
        val metadata = metadata(uri)
        require(metadata.size == null || metadata.size <= MAX_PDF_BYTES) { "File exceeds the 10 MB import limit." }
        val mime = context.contentResolver.getType(uri).orEmpty()
        if (mime == PDF_MIME || metadata.name.endsWith(".pdf", ignoreCase = true)) importPdf(uri, metadata.name)
        else importText(uri, metadata.name, mime.ifBlank { "text/plain" })
    }

    private fun importPdf(uri: Uri, name: String): ImportedDocument {
        PDFBoxResourceLoader.init(context.applicationContext)
        val input = context.contentResolver.openInputStream(uri) ?: error("The selected PDF could not be opened.")
        return input.use { stream ->
            PDDocument.load(stream).use { document ->
                require(document.numberOfPages in 1..MAX_PAGES) { "PDF must contain between 1 and $MAX_PAGES pages." }
                require(!document.isEncrypted) { "Password-protected PDFs are not supported." }
                val stripper = PDFTextStripper()
                val content = buildString {
                    for (page in 1..document.numberOfPages) {
                        stripper.startPage = page
                        stripper.endPage = page
                        appendLine("[PAGE $page]")
                        appendLine(stripper.getText(document).trim())
                        appendLine()
                    }
                }.trim()
                require(content.replace(Regex("\\[PAGE \\d+]"), "").isNotBlank()) {
                    "This PDF has no searchable text. Scanned-page OCR is not available in this build."
                }
                ImportedDocument(name, content, document.numberOfPages, PDF_MIME)
            }
        }
    }

    private fun importText(uri: Uri, name: String, mime: String): ImportedDocument {
        val output = ByteArrayOutputStream()
        context.contentResolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                require(output.size() + read <= MAX_TEXT_BYTES) { "Text file exceeds the 2 MB import limit." }
                output.write(buffer, 0, read)
            }
        } ?: error("The selected document could not be opened.")
        val text = output.toString(Charsets.UTF_8.name())
        require(text.isNotBlank()) { "The selected document contains no readable text." }
        return ImportedDocument(name, text, null, mime)
    }

    private fun metadata(uri: Uri): Metadata {
        var name = "Imported notebook"
        var size: Long? = null
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                name = cursor.getString(0) ?: name
                if (!cursor.isNull(1)) size = cursor.getLong(1)
            }
        }
        return Metadata(name, size)
    }

    private data class Metadata(val name: String, val size: Long?)

    companion object {
        private const val PDF_MIME = "application/pdf"
        private const val MAX_TEXT_BYTES = 2 * 1024 * 1024
        private const val MAX_PDF_BYTES = 10 * 1024 * 1024
        private const val MAX_PAGES = 200
    }
}
