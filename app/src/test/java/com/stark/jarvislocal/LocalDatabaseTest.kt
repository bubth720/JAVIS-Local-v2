package com.stark.jarvislocal

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 25, 35], application = Application::class, manifest = Config.NONE)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class LocalDatabaseTest {
    private lateinit var context: Context
    private lateinit var helper: LocalDatabase
    private val databaseName = "jarvis_local_v2.db"

    @Before
    fun setUp() {
        // Robolectric supplies isolated synthetic storage, never an installed user's database.
        context = RuntimeEnvironment.getApplication()
        helper = LocalDatabase(context)
    }

    @After
    fun tearDown() {
        helper.close()
    }

    @Test
    fun freshDatabasePersistsAllFourCollectionsAfterReopening() {
        val message = ChatMessage(role = "user", text = "Bonjour", createdAt = 1234L)
        val messageId = helper.addMessage(message)
        val memoryId = helper.addMemory("Un souvenir")
        val documentId = helper.addDocument("Nouveau.pdf", "application/pdf", listOf("Un", "Deux"))
        assertTrue(messageId > 0)
        assertTrue(memoryId > 0)
        assertTrue(documentId > 0)
        val before = snapshot(helper.readableDatabase)
        reopenHelper()
        assertEquals(2, helper.readableDatabase.version)
        assertEquals(before, snapshot(helper.readableDatabase))
        assertEquals(listOf(message.copy(id = messageId)), helper.recentMessages())
        assertEquals("Un souvenir", helper.allMemories().single().text)
        assertEquals(listOf("Nouveau.pdf"), helper.documentNames())
        assertEquals(listOf("Un", "Deux"), helper.allChunks().sortedBy { it.chunkIndex }.map { it.text })
        assertTrue(helper.allChunks().all { it.documentId == documentId })
    }

    @Test
    fun versionOneMigrationPreservesEveryValueAndIdentifier() {
        val before = createHistoricalDatabase(1)
        assertEquals(2, helper.writableDatabase.version)
        assertEquals(before, snapshot(helper.readableDatabase))
        assertHistoricalReads()
        reopenHelper()
        assertEquals(before, snapshot(helper.readableDatabase))
        assertHistoricalReads()
    }

    @Test
    fun existingVersionTwoPreservesEveryValueAndIdentifier() {
        val before = createHistoricalDatabase(2)
        assertEquals(2, helper.writableDatabase.version)
        assertEquals(before, snapshot(helper.readableDatabase))
        assertHistoricalReads()
        reopenHelper()
        assertEquals(before, snapshot(helper.readableDatabase))
    }

    @Test
    fun migrationPreservesAutoincrementHighWaterMarksAndAllowsNewWrites() {
        createHistoricalDatabase(1)
        openRawDatabase().use { db ->
            db.execSQL("UPDATE sqlite_sequence SET seq = 200")
        }
        val before = openRawDatabase().use(::snapshot)
        assertEquals(before, snapshot(helper.writableDatabase))
        assertEquals(201L, helper.addMessage(ChatMessage(role = "user", text = "Suite")))
        assertEquals(201L, helper.addMemory("Autre souvenir"))
        assertEquals(201L, helper.addDocument("Suite.pdf", null, listOf("Suite")))
        assertEquals(201L, helper.allChunks().single { it.documentId == 201L }.id)
        assertEquals(3, helper.recentMessages().size)
        assertEquals(2, helper.allMemories().size)
        assertEquals(3, helper.documentNames().size)
        assertEquals(4, helper.allChunks().size)
    }

    @Test
    fun rejectedMessageInsertIsReportedWithoutChangingStoredData() {
        createHistoricalDatabase(2)
        val db = helper.writableDatabase
        db.execSQL("CREATE TRIGGER reject_message BEFORE INSERT ON messages BEGIN SELECT RAISE(ABORT, 'test rejection'); END")
        val before = snapshot(db)
        assertThrows(SQLiteException::class.java) {
            helper.addMessage(ChatMessage(role = "user", text = "Non enregistré"))
        }
        assertEquals(before, snapshot(db))
    }

    @Test
    fun ignoredMemoryInsertIsReportedWithoutChangingStoredData() {
        createHistoricalDatabase(2)
        val db = helper.writableDatabase
        db.execSQL("CREATE TRIGGER ignore_memory BEFORE INSERT ON memories BEGIN SELECT RAISE(IGNORE); END")
        val before = snapshot(db)
        assertThrows(SQLiteException::class.java) { helper.addMemory("Non enregistré") }
        assertEquals(before, snapshot(db))
    }

    @Test
    fun rejectedDocumentInsertDoesNotCreateChunksOrChangeExistingData() {
        createHistoricalDatabase(2)
        val db = helper.writableDatabase
        db.execSQL("CREATE TRIGGER reject_document BEFORE INSERT ON documents BEGIN SELECT RAISE(ABORT, 'test rejection'); END")
        val before = snapshot(db)
        assertThrows(SQLiteException::class.java) {
            helper.addDocument("Échec.pdf", null, listOf("Passage"))
        }
        assertEquals(before, snapshot(db))
        assertTrue(!db.inTransaction())
    }

    @Test
    fun rejectedSecondChunkRollsBackTheDocumentAndFirstChunk() {
        assertFailedChunkImport("ABORT, 'test rejection'")
    }

    @Test
    fun ignoredSecondChunkRollsBackTheDocumentAndFirstChunk() {
        assertFailedChunkImport("IGNORE")
    }

    @Test
    fun unsupportedUpgradeKeepsTheOriginalVersionAndData() {
        val before = createHistoricalDatabase(1)
        val unsupported = object : SQLiteOpenHelper(context, databaseName, null, 3) {
            override fun onCreate(db: SQLiteDatabase) = error("Existing fixture expected")
            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
                helper.onUpgrade(db, oldVersion, newVersion)
            }
        }
        try {
            assertThrows(SQLiteException::class.java) { unsupported.writableDatabase }
        } finally {
            unsupported.close()
        }
        openRawDatabase().use { db ->
            assertEquals(1, db.version)
            assertEquals(before, snapshot(db))
        }
    }

    @Test
    fun unsupportedDowngradeKeepsTheOriginalVersionAndData() {
        // Synthetic future version: verify SQLiteOpenHelper's non-destructive refusal.
        val before = createHistoricalDatabase(3)
        assertThrows(SQLiteException::class.java) { helper.writableDatabase }
        openRawDatabase().use { db ->
            assertEquals(3, db.version)
            assertEquals(before, snapshot(db))
        }
    }

    private fun assertFailedChunkImport(raise: String) {
        createHistoricalDatabase(2)
        val db = helper.writableDatabase
        db.execSQL("CREATE TRIGGER reject_chunk BEFORE INSERT ON chunks WHEN NEW.chunk_index = 1 BEGIN SELECT RAISE($raise); END")
        val before = snapshot(db)
        assertThrows(SQLiteException::class.java) {
            helper.addDocument("Échec.pdf", "application/pdf", listOf("Premier", "Deuxième"))
        }
        assertEquals(before, snapshot(db))
        assertTrue(!db.inTransaction())
        db.execSQL("DROP TRIGGER reject_chunk")
        assertTrue(helper.addDocument("Reprise.pdf", null, listOf("Passage")) > 0)
    }

    private fun createHistoricalDatabase(version: Int): Map<String, List<List<String?>>> {
        val fixture = requireNotNull(javaClass.getResourceAsStream("/sqlite/schema-v1-v2.sql"))
            .bufferedReader().use { it.readText() }
        return openRawDatabase().use { db ->
            fixture.lineSequence().filterNot { it.trimStart().startsWith("--") }
                .joinToString("\n").split(';').filter { it.isNotBlank() }
                .forEach(db::execSQL)
            db.version = version
            snapshot(db)
        }
    }

    private fun openRawDatabase(): SQLiteDatabase {
        val file = context.getDatabasePath(databaseName)
        file.parentFile?.mkdirs()
        return SQLiteDatabase.openOrCreateDatabase(file, null)
    }

    private fun reopenHelper() {
        helper.close()
        helper = LocalDatabase(context)
    }

    private fun snapshot(db: SQLiteDatabase): Map<String, List<List<String?>>> =
        listOf("messages", "memories", "documents", "chunks", "sqlite_sequence").associateWith { table ->
            val order = if (table == "sqlite_sequence") "name" else "id"
            db.rawQuery("SELECT * FROM $table ORDER BY $order", null).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add((0 until cursor.columnCount).map { column ->
                            if (cursor.isNull(column)) null else cursor.getString(column)
                        })
                    }
                }
            }
        }

    private fun assertHistoricalReads() {
        assertEquals(
            listOf(
                ChatMessage(11, "user", "Où est mon document ?", 1700000000000L, null),
                ChatMessage(12, "assistant", "Voici le passage demandé.", 1700000000001L, "Résumé.pdf")
            ), helper.recentMessages()
        )
        assertEquals(listOf(MemoryItem(21, "Mon projet s'appelle JARVIS.", 1700000000002L)), helper.allMemories())
        assertEquals(listOf("Notes.pdf", "Résumé.pdf"), helper.documentNames())
        assertEquals(
            listOf(
                DocumentChunk(41, 31, "Résumé.pdf", 0, "[PAGE 1] Premier passage."),
                DocumentChunk(42, 31, "Résumé.pdf", 1, "Deuxième passage."),
                DocumentChunk(43, 32, "Notes.pdf", 0, "Une note privée.")
            ), helper.allChunks().sortedBy { it.id }
        )
    }
}
