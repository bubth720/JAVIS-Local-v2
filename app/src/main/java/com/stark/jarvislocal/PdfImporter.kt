package com.stark.jarvislocal

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

class PdfImporter(private val context: Context, private val db: LocalDatabase) {
    fun import(uri: Uri, displayName: String, mime: String?): Int {
        val allChunks = mutableListOf<String>()

        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Fichier inaccessible" }

            PDDocument.load(input).use { pdf ->
                val stripper = PDFTextStripper().apply {
                    setSortByPosition(true)
                }

                for (page in 1..pdf.numberOfPages) {
                    stripper.startPage = page
                    stripper.endPage = page

                    val pageText = stripper.getText(pdf)
                        .replace('\u0000', ' ')
                        .trim()

                    if (pageText.isNotBlank()) {
                        val pageChunks = TextChunker.chunk(
                            text = "[PAGE $page]\n$pageText",
                            targetChars = 1000,
                            overlapChars = 180
                        )
                        allChunks += pageChunks
                    }
                }
            }
        }

        require(allChunks.isNotEmpty()) {
            "Aucun texte exploitable. Le PDF est probablement scanné ou composé uniquement d'images : un module OCR local est nécessaire."
        }

        db.addDocument(displayName, mime, allChunks)
        return allChunks.size
    }
}
