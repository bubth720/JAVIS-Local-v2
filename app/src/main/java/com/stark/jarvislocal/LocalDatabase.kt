package com.stark.jarvislocal

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class LocalDatabase(context: Context) : SQLiteOpenHelper(context, "jarvis_local_v2.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE messages(id INTEGER PRIMARY KEY AUTOINCREMENT, role TEXT NOT NULL, text TEXT NOT NULL, source TEXT, created_at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE memories(id INTEGER PRIMARY KEY AUTOINCREMENT, text TEXT NOT NULL, created_at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE documents(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, mime TEXT, created_at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE chunks(id INTEGER PRIMARY KEY AUTOINCREMENT, document_id INTEGER NOT NULL, document_name TEXT NOT NULL, chunk_index INTEGER NOT NULL, text TEXT NOT NULL)")
        db.execSQL("CREATE INDEX idx_chunks_doc ON chunks(document_id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun addMessage(m: ChatMessage): Long = writableDatabase.insert("messages", null, ContentValues().apply {
        put("role", m.role); put("text", m.text); put("source", m.source); put("created_at", m.createdAt)
    })

    fun recentMessages(limit: Int = 30): List<ChatMessage> {
        val out = mutableListOf<ChatMessage>()
        readableDatabase.rawQuery(
            "SELECT id,role,text,source,created_at FROM messages ORDER BY id DESC LIMIT ?",
            arrayOf(limit.toString())
        ).use { c ->
            while (c.moveToNext()) out += ChatMessage(c.getLong(0), c.getString(1), c.getString(2), c.getLong(4), c.getString(3))
        }
        return out.reversed()
    }

    fun addMemory(text: String): Long = writableDatabase.insert("memories", null, ContentValues().apply {
        put("text", text); put("created_at", System.currentTimeMillis())
    })

    fun allMemories(): List<MemoryItem> {
        val out = mutableListOf<MemoryItem>()
        readableDatabase.rawQuery("SELECT id,text,created_at FROM memories ORDER BY id DESC", null).use { c ->
            while (c.moveToNext()) out += MemoryItem(c.getLong(0), c.getString(1), c.getLong(2))
        }
        return out
    }

    fun addDocument(name: String, mime: String?, chunks: List<String>): Long {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val docId = db.insert("documents", null, ContentValues().apply {
                put("name", name); put("mime", mime); put("created_at", System.currentTimeMillis())
            })
            chunks.forEachIndexed { index, text ->
                db.insert("chunks", null, ContentValues().apply {
                    put("document_id", docId); put("document_name", name); put("chunk_index", index); put("text", text)
                })
            }
            db.setTransactionSuccessful()
            docId
        } finally { db.endTransaction() }
    }

    fun allChunks(): List<DocumentChunk> {
        val out = mutableListOf<DocumentChunk>()
        readableDatabase.rawQuery("SELECT id,document_id,document_name,chunk_index,text FROM chunks", null).use { c ->
            while (c.moveToNext()) out += DocumentChunk(c.getLong(0), c.getLong(1), c.getString(2), c.getInt(3), c.getString(4))
        }
        return out
    }

    fun documentNames(): List<String> {
        val out = mutableListOf<String>()
        readableDatabase.rawQuery("SELECT name FROM documents ORDER BY id DESC", null).use { c ->
            while (c.moveToNext()) out += c.getString(0)
        }
        return out
    }
}
