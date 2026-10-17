package com.stark.jarvislocal.fixture

import android.util.AtomicFile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.security.MessageDigest

/** Only technical identifiers/phase are stored; never request content or a credential. */
internal class AtomicPilotJournal(file: File) : PilotJournal {
    private val atomic = AtomicFile(file)
    override fun read(): PilotRecord? {
        val bytes = try {
            atomic.openRead().use { input ->
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(512)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    requirePilot(out.size() + count <= 4096, "JOURNAL_TOO_LARGE")
                    out.write(buffer, 0, count)
                }
                out.toByteArray()
            }
        } catch (missing: java.io.FileNotFoundException) {
            // AtomicFile may have restored its backup. An existing unreadable journal is not empty.
            requirePilot(!atomic.baseFile.exists() && !File(atomic.baseFile.path + ".bak").exists(), "JOURNAL_UNREADABLE")
            return null
        }
        requirePilot(bytes.size > 32, "JOURNAL_CORRUPT")
        val payload = bytes.copyOfRange(32, bytes.size)
        requirePilot(MessageDigest.isEqual(bytes.copyOfRange(0,32), sha(payload)), "JOURNAL_CORRUPT")
        return DataInputStream(ByteArrayInputStream(payload)).use { input ->
            requirePilot(input.readInt() == 1, "JOURNAL_VERSION_UNSUPPORTED")
            val owner = if (input.readBoolean()) input.readUTF() else null
            val key = input.key()
            val generation = input.readLong()
            val phase = PilotPhase.valueOf(input.readUTF())
            val record = PilotRecord(owner, key, generation, phase, input.readLong(), input.readBoolean(), input.key())
            requirePilot(input.available() == 0 && generation in 0..9007199254740991L &&
                (owner == null) == (key == null) && (owner == null || owner.matches(Regex("[A-Za-z0-9._-]{1,80}"))) &&
                (key == null || key.generation == generation), "JOURNAL_CORRUPT")
            record
        }
    }

    override fun write(record: PilotRecord) {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).use {
            it.writeInt(1); it.writeBoolean(record.owner != null); record.owner?.let(it::writeUTF)
            it.key(record.key); it.writeLong(record.generation); it.writeUTF(record.phase.name)
            it.writeLong(record.deadline); it.writeBoolean(record.cancelled); it.key(record.lastReleased)
        }
        val payload = out.toByteArray()
        requirePilot(payload.size + 32 <= 4096, "JOURNAL_TOO_LARGE")
        val stream = atomic.startWrite()
        try { stream.write(sha(payload)); stream.write(payload); stream.fd.sync(); atomic.finishWrite(stream) }
        catch (failure: Throwable) { atomic.failWrite(stream); throw failure }
    }

    private fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes)
    private fun DataOutputStream.key(key: PilotKey?) {
        writeBoolean(key != null)
        key?.let { for (id in listOf(it.sessionId,it.operationId,it.correlationId,it.epoch,it.cancellationId,it.authorizationRef,it.leaseId)) writeUTF(id); writeLong(it.generation) }
    }
    private fun DataInputStream.key(): PilotKey? {
        if (!readBoolean()) return null
        val ids = List(7) { readUTF() }
        requirePilot(ids.all(::canonicalUuid), "JOURNAL_CORRUPT")
        val generation = readLong(); requirePilot(generation in 1..9007199254740991L, "JOURNAL_CORRUPT")
        return PilotKey(ids[0],ids[1],ids[2],ids[3],ids[4],ids[5],ids[6],generation)
    }
}
