package com.stark.jarvislocal

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

class PdfImporter(private val context: Context, private val db: LocalDatabase) {
    fun import(uri: Uri, displayName: String, mime: String?): Int {
        val text = context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Fichier inaccessible" }
            PDDocument.load(input).use { pdf -> PDFTextStripper().getText(pdf) }
        }
        val chunks = TextChunker.chunk(text)
        require(chunks.isNotEmpty()) {
            "Aucun texte exploitable. Le PDF est peut-être scanné : un module OCR sera nécessaire."
        }
        db.addDocument(displayName, mime, chunks)
        return chunks.size
    }
}
