package com.stark.jarvislocal.memory

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import java.io.Closeable
import java.io.File
import java.util.UUID

/** Existing Android SQLite engine; isolated namespace for review, NEVER a historical database migration. */
class ConfirmedMemoryStore(context: Context) : Closeable {
    private class Store(context: Context) : SQLiteOpenHelper(context,
        File(context.noBackupFilesDir, "confirmed_memory_lab.sqlite").absolutePath, null, 1,
        android.database.DatabaseErrorHandler { throw SQLiteException("CORRUPT_MEMORY_DATABASE_NO_DELETE") }) {
        override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE confirmed_memory(id TEXT PRIMARY KEY, revision INTEGER NOT NULL, content TEXT NOT NULL, scope TEXT NOT NULL, preference_key TEXT UNIQUE, created_at INTEGER NOT NULL, expires_at INTEGER, suspended INTEGER NOT NULL DEFAULT 0, receipt_id TEXT NOT NULL)")
            db.execSQL("CREATE TABLE consent_receipts(id TEXT PRIMARY KEY, nonce TEXT UNIQUE NOT NULL, memory_id TEXT NOT NULL REFERENCES confirmed_memory(id) ON DELETE CASCADE, session_id TEXT NOT NULL, content_digest TEXT NOT NULL, confirmed_at INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE memory_audit(sequence INTEGER PRIMARY KEY AUTOINCREMENT, memory_id TEXT NOT NULL, operation TEXT NOT NULL)")
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) { throw SQLiteException("MEMORY_MIGRATION_NOT_AUTHORIZED") }
    }
    private val helper = Store(context.applicationContext)
    @Synchronized internal fun confirm(candidate: PendingMemoryCandidate, confirmedAt: Long, checkConsent: () -> Unit): MemoryRecord = transaction { db ->
        checkConsent(); validateContent(candidate.content)
        require(confirmedAt >= 0 && (candidate.expiresAtMs == null || confirmedAt < candidate.expiresAtMs))
        val replay = db.rawQuery("SELECT id FROM consent_receipts WHERE nonce=?", arrayOf(candidate.id)).use { it.moveToFirst() }
        if (replay) throw MemoryRejected("CONSENT_REPLAY_OR_UNKNOWN_RESULT")
        if (candidate.replaces == null && candidate.preferenceKey != null && findPreference(candidate.preferenceKey) != null)
            throw MemoryRejected("PREFERENCE_CONFLICT_REVIEW_REQUIRED")
        val current = candidate.replaces?.let { read(db, it.id).also { row -> if (row.reference != it) throw MemoryRejected("REVISION_CONFLICT") } }
        if (current == null && count(db, "confirmed_memory") >= 200) throw MemoryRejected("MEMORY_QUOTA")
        val id = current?.reference?.id ?: UUID.randomUUID().toString()
        val revision = (current?.reference?.revision ?: 0) + 1
        val receipt = ConsentReceipt(UUID.randomUUID().toString(), candidate.id, candidate.sessionId, contentDigest(candidate.content), confirmedAt)
        val values = ContentValues().apply {
            put("id", id); put("revision", revision); put("content", candidate.content); put("scope", candidate.scope.name)
            put("preference_key", candidate.preferenceKey); put("created_at", confirmedAt)
            if (candidate.expiresAtMs == null) putNull("expires_at") else put("expires_at", candidate.expiresAtMs)
            put("suspended", 0); put("receipt_id", receipt.id)
        }
        if (current == null) db.insertOrThrow("confirmed_memory", null, values)
        else check(db.update("confirmed_memory", values, "id=?", arrayOf(id)) == 1)
        db.insertOrThrow("consent_receipts", null, ContentValues().apply {
            put("id", receipt.id); put("nonce", receipt.nonce); put("memory_id", id); put("session_id", receipt.sessionId)
            put("content_digest", receipt.contentDigest); put("confirmed_at", confirmedAt)
        })
        audit(db, id, if (current == null) "CREATED" else "EDITED")
        checkConsent()
        read(db, id)
    }
    @Synchronized fun readOwned(id: String): MemoryRecord = read(helper.readableDatabase, id)
    @Synchronized fun listOwned(): List<MemoryRecord> = helper.readableDatabase.rawQuery("SELECT id FROM confirmed_memory ORDER BY created_at DESC,id LIMIT 200", null).use {
        buildList { while (it.moveToNext()) add(read(helper.readableDatabase, it.getString(0))) }
    }
    @Synchronized internal fun findPreference(key: String): MemoryRecord? = helper.readableDatabase.rawQuery("SELECT id FROM confirmed_memory WHERE preference_key=?", arrayOf(key)).use {
        if (it.moveToFirst()) read(helper.readableDatabase, it.getString(0)) else null
    }
    /** Only explicit owner UI may call these mutations, never a document/LLM response. */
    @Synchronized fun deleteOwned(expected: MemoryReference) = transaction { db ->
        if (read(db, expected.id).reference != expected) throw MemoryRejected("REVISION_CONFLICT")
        audit(db, expected.id, "DELETED")
        check(db.delete("confirmed_memory", "id=?", arrayOf(expected.id)) == 1)
        Unit
    }
    @Synchronized fun suspendOwned(expected: MemoryReference, value: Boolean) = transaction { db ->
        if (read(db, expected.id).reference != expected) throw MemoryRejected("REVISION_CONFLICT")
        check(db.update("confirmed_memory", ContentValues().apply { put("suspended", if (value) 1 else 0); put("revision", expected.revision + 1) }, "id=?", arrayOf(expected.id)) == 1)
        audit(db, expected.id, if (value) "SUSPENDED" else "RESUMED")
        Unit
    }
    @Synchronized fun auditOperations(): List<String> = helper.readableDatabase.rawQuery("SELECT operation FROM memory_audit ORDER BY sequence", null).use {
        buildList { while (it.moveToNext()) add(it.getString(0)) }
    }
    private fun count(db: SQLiteDatabase, table: String): Int = db.rawQuery("SELECT COUNT(*) FROM $table", null).use { it.moveToFirst(); it.getInt(0) }
    private fun audit(db: SQLiteDatabase, id: String, operation: String) {
        if (count(db, "memory_audit") >= 2000) throw MemoryRejected("AUDIT_QUOTA")
        db.insertOrThrow("memory_audit", null, ContentValues().apply { put("memory_id", id); put("operation", operation) })
    }
    private fun read(db: SQLiteDatabase, id: String): MemoryRecord {
        require(UUID.fromString(id).toString() == id)
        val sql = "SELECT m.revision,m.content,m.scope,m.preference_key,m.created_at,m.expires_at,m.suspended,r.id,r.nonce,r.session_id,r.content_digest,r.confirmed_at,length(m.content) FROM confirmed_memory m JOIN consent_receipts r ON r.id=m.receipt_id AND r.memory_id=m.id WHERE m.id=?"
        return db.rawQuery(sql, arrayOf(id)).use { c ->
            if (!c.moveToFirst()) throw MemoryRejected("MEMORY_ABSENT_OR_UNCONFIRMED")
            try {
                require(c.getLong(12) <= 1500)
                require(c.getLong(4) >= 0 && c.getLong(11) == c.getLong(4) && c.getInt(6) in 0..1)
                require(c.isNull(5) || c.getLong(5) > c.getLong(4))
                require(c.isNull(3) || c.getString(3).matches(Regex("[a-z][a-z0-9_.-]{0,63}")))
                val text = c.getString(1); validateContent(text)
                require(contentDigest(text) == c.getString(10) && c.getLong(0) > 0)
                listOf(c.getString(7), c.getString(8), c.getString(9)).forEach { require(UUID.fromString(it).toString() == it) }
                val receipt = ConsentReceipt(c.getString(7), c.getString(8), c.getString(9), c.getString(10), c.getLong(11))
                MemoryRecord(MemoryReference(id, c.getLong(0), receipt.contentDigest), text, MemoryScope.valueOf(c.getString(2)),
                    if (c.isNull(3)) null else c.getString(3), c.getLong(4), if (c.isNull(5)) null else c.getLong(5), c.getInt(6) != 0, receipt)
            } catch (_: Exception) { throw MemoryRejected("CORRUPT_MEMORY") }
        }
    }
    private fun <T> transaction(action: (SQLiteDatabase) -> T): T {
        val db = helper.writableDatabase
        db.beginTransaction()
        return try { val result = action(db); db.setTransactionSuccessful(); result } finally { db.endTransaction() }
    }
    @Synchronized override fun close() = helper.close()
}
