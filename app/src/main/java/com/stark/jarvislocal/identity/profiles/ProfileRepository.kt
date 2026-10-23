package com.stark.jarvislocal.identity.profiles

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import java.io.Closeable
import java.io.File
import java.util.UUID

/** Explicit laboratory store, never opens/imports/migrates LocalDatabase or historical preferences. */
enum class ProfileRevisionOrigin { INITIAL, USER_EDIT, RESTORE }
data class ProfileRevisionInfo(val revision: Long, val origin: ProfileRevisionOrigin, val restoredFrom: Long?)

class ProfileRepository(context: Context) : Closeable {
    private val helper = Store(context.applicationContext)
    private class Store(context: Context) : SQLiteOpenHelper(context,
        File(context.noBackupFilesDir, "identity_profiles_lab.sqlite").absolutePath, null, 1,
        android.database.DatabaseErrorHandler { throw SQLiteException("CORRUPT_LAB_PROFILE_DATABASE_NO_DELETE") }) {
        override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE profiles(id TEXT PRIMARY KEY, current_revision INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE profile_revisions(profile_id TEXT NOT NULL REFERENCES profiles(id), revision INTEGER NOT NULL, payload BLOB NOT NULL, digest TEXT NOT NULL, origin TEXT NOT NULL, restored_from INTEGER, PRIMARY KEY(profile_id,revision))")
            db.execSQL("CREATE TABLE profile_selection(singleton INTEGER PRIMARY KEY CHECK(singleton=1), profile_id TEXT NOT NULL REFERENCES profiles(id))")
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) { throw SQLiteException("LAB_PROFILE_MIGRATION_NOT_AUTHORIZED") }
    }
    @Synchronized fun create(draft: ProfileDraft): IdentityProfile = transaction { db ->
        require(count(db) < 16) { "PROFILE_QUOTA" }
        val id = UUID.randomUUID().toString()
        db.insertOrThrow("profiles", null, ContentValues().apply { put("id", id); put("current_revision", 1L) })
        insert(db, id, 1, draft, "INITIAL", null)
    }
    @Synchronized fun read(id: String, revision: Long? = null): IdentityProfile = read(helper.readableDatabase, id, revision)
    @Synchronized fun list(): List<ProfileReference> = helper.readableDatabase.rawQuery("SELECT id FROM profiles ORDER BY id", null).use { cursor ->
        buildList { while (cursor.moveToNext()) add(read(helper.readableDatabase, cursor.getString(0), null).reference) }
    }
    @Synchronized fun revise(expected: ProfileReference, draft: ProfileDraft): IdentityProfile = transaction { db ->
        requireCurrent(db, expected)
        insert(db, expected.id, expected.revision + 1, draft, "USER_EDIT", null)
    }
    @Synchronized fun restore(expected: ProfileReference, targetRevision: Long): IdentityProfile = transaction { db ->
        requireCurrent(db, expected)
        val previous = read(db, expected.id, targetRevision)
        insert(db, expected.id, expected.revision + 1, previous.draft, "RESTORE", targetRevision)
    }
    @Synchronized fun select(expected: ProfileReference) = transaction { db ->
        requireCurrent(db, expected)
        db.insertWithOnConflict("profile_selection", null, ContentValues().apply {
            put("singleton", 1); put("profile_id", expected.id)
        }, SQLiteDatabase.CONFLICT_REPLACE).also { if (it == -1L) throw ProfileRejected("SELECTION_FAILED") }
        Unit
    }
    @Synchronized fun history(id: String): List<ProfileRevisionInfo> {
        read(id)
        return helper.readableDatabase.rawQuery("SELECT revision,origin,restored_from FROM profile_revisions WHERE profile_id=? ORDER BY revision", arrayOf(id)).use {
            buildList { while (it.moveToNext()) add(ProfileRevisionInfo(it.getLong(0), ProfileRevisionOrigin.valueOf(it.getString(1)), if (it.isNull(2)) null else it.getLong(2))) }
        }
    }
    @Synchronized fun selected(): IdentityProfile? = helper.readableDatabase.rawQuery("SELECT profile_id FROM profile_selection WHERE singleton=1", null).use {
        if (it.moveToFirst()) read(helper.readableDatabase, it.getString(0), null) else null
    }
    private fun count(db: SQLiteDatabase): Int = db.rawQuery("SELECT COUNT(*) FROM profiles", null).use { it.moveToFirst(); it.getInt(0) }
    private fun requireCurrent(db: SQLiteDatabase, expected: ProfileReference) {
        if (read(db, expected.id, null).reference != expected) throw ProfileRejected("REVISION_CONFLICT")
    }
    private fun insert(db: SQLiteDatabase, id: String, revision: Long, draft: ProfileDraft, origin: String, restored: Long?): IdentityProfile {
        require(revision in 1..64) { "REVISION_QUOTA" }
        val bytes = ProfileCodec.encode(id, revision, draft)
        val digest = ProfileCodec.digest(bytes)
        db.insertOrThrow("profile_revisions", null, ContentValues().apply {
            put("profile_id", id); put("revision", revision); put("payload", bytes); put("digest", digest)
            put("origin", origin); if (restored != null) put("restored_from", restored)
        })
        check(db.update("profiles", ContentValues().apply { put("current_revision", revision) }, "id=?", arrayOf(id)) == 1)
        return ProfileCodec.decode(bytes, ProfileReference(id, revision, digest))
    }
    private fun read(db: SQLiteDatabase, id: String, revision: Long?): IdentityProfile {
        require(UUID.fromString(id).toString() == id)
        val version = revision ?: db.rawQuery("SELECT current_revision FROM profiles WHERE id=?", arrayOf(id)).use {
            if (!it.moveToFirst()) throw ProfileRejected("PROFILE_ABSENT")
            it.getLong(0)
        }
        return db.rawQuery("SELECT payload,digest,length(payload) FROM profile_revisions WHERE profile_id=? AND revision=?", arrayOf(id, version.toString())).use {
            if (!it.moveToFirst()) throw ProfileRejected("REVISION_ABSENT")
            if (it.getLong(2) > 32768) throw ProfileRejected("CORRUPT_PROFILE")
            try { ProfileCodec.decode(it.getBlob(0), ProfileReference(id, version, it.getString(1))) }
            catch (_: Exception) { throw ProfileRejected("CORRUPT_PROFILE") }
        }
    }
    private fun <T> transaction(action: (SQLiteDatabase) -> T): T {
        val db = helper.writableDatabase
        db.beginTransaction()
        return try { val result = action(db); db.setTransactionSuccessful(); result } finally { db.endTransaction() }
    }
    @Synchronized override fun close() = helper.close()
}
