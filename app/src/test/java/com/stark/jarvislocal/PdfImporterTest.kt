package com.stark.jarvislocal

import android.app.Application
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 35], application = Application::class, manifest = Config.NONE)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class PdfImporterTest {
    @Test fun syntheticPdfKeepsDocumentAndPageOnEveryChunkAfterReopening() {
        val context = RuntimeEnvironment.getApplication()
        PDFBoxResourceLoader.init(context)
        val file = File(context.cacheDir, "synthetic.pdf")
        PDDocument().use { document ->
            for (number in 1..2) {
                val page = PDPage(); document.addPage(page)
                PDPageContentStream(document, page).use { content ->
                    content.beginText(); content.setFont(PDType1Font.HELVETICA, 10f); content.newLineAtOffset(30f, 740f)
                    repeat(35) { line ->
                        content.showText("ATLAS page $number line $line synthetic passage preservation check.")
                        content.newLineAtOffset(0f, -15f)
                    }
                    content.endText()
                }
            }
            document.save(file)
        }
        var db = LocalDatabase(context)
        try {
            val count = PdfImporter(context, db).import(Uri.fromFile(file), "Atlas.pdf", "application/pdf")
            val before = db.allChunks()
            assertTrue(count > 2)
            assertTrue(before.all { it.documentName == "Atlas.pdf" && it.text.startsWith("[PAGE ") })
            assertEquals(setOf("[PAGE 1]", "[PAGE 2]"), before.map { it.text.substringBefore('\n') }.toSet())
            db.close(); db = LocalDatabase(context)
            assertEquals(before, db.allChunks())
            assertEquals(listOf("Atlas.pdf"), db.documentNames())
        } finally { db.close(); file.delete() }
    }

    @Test fun imageOnlyOrEmptyPdfDoesNotAlterExistingCorpus() {
        val context = RuntimeEnvironment.getApplication()
        PDFBoxResourceLoader.init(context)
        val file = File(context.cacheDir, "empty.pdf")
        PDDocument().use { document -> document.addPage(PDPage()); document.save(file) }
        val db = LocalDatabase(context)
        try {
            db.addDocument("Existing.pdf", "application/pdf", listOf("old source"))
            val before = db.allChunks()
            assertThrows(IllegalArgumentException::class.java) {
                PdfImporter(context, db).import(Uri.fromFile(file), "Empty.pdf", "application/pdf")
            }
            assertEquals(before, db.allChunks()); assertEquals(listOf("Existing.pdf"), db.documentNames())
        } finally { db.close(); file.delete() }
    }
}
