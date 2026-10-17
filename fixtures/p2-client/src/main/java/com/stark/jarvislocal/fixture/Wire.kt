// Codec subset from LLM 003E 5751646acd4a7bb13caaea4b96a4ab3a68ec6adb; no engine/domain dependency.
package com.stark.jarvislocal.fixture

import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import java.io.*
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest

class Refusal(val code: String) : IllegalArgumentException(code)
fun demand(ok: Boolean, code: String) { if (!ok) throw Refusal(code) }
object Wire {
    const val VERSION = "jarvis.fixture.p2/1"
    const val CLIENT = "fr.jarvis.fixture.core"
    const val ENDPOINT = "fr.jarvis.fixture.llm"
    const val PERMISSION = "fr.jarvis.fixture.llm.permission.BIND_P2"
    val contract: String get() = BuildConfig.CONTRACT_SHA
    val keyNames = setOf("version","contract","epoch","session_id","operation_id","correlation_id","cancellation_id","authorization_ref","lease_id","generation")
    val requestNames = keyNames + setOf("system","question","model","model_sha256","profile","context_tokens","output_tokens","remaining_ms","synthetic")
    fun decode(bytes: ByteArray, names: Set<String>): Map<String,String> {
        demand(bytes.size <= 24576,"INPUT_TOO_LARGE")
        val text = try { Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString() } catch (_: Exception) { throw Refusal("INVALID_UTF8") }
        try {
            val fields = linkedMapOf<String,String>()
            JsonReader(StringReader(text)).use { r ->
                r.isLenient = false; r.beginObject()
                while (r.hasNext()) {
                    val name = r.nextName()
                    demand(name in names && name !in fields,"UNKNOWN_OR_DUPLICATE_FIELD")
                    demand(r.peek() == JsonToken.STRING,"INVALID_TYPE")
                    fields[name] = r.nextString()
                }
                r.endObject(); demand(r.peek() == JsonToken.END_DOCUMENT,"TRAILING_DATA")
            }
            demand(fields.keys == names,"MISSING_FIELD")
            return fields.toMap()
        } catch (e: Refusal) { throw e } catch (_: Exception) { throw Refusal("MALFORMED") }
    }
    fun encode(fields: Map<String,String>): ByteArray {
        val s = StringWriter()
        JsonWriter(s).use { w -> w.beginObject(); fields.toSortedMap().forEach { (k,v) -> w.name(k).value(v) }; w.endObject() }
        val bytes = s.toString().toByteArray(Charsets.UTF_8); demand(bytes.size <= 24576,"OUTPUT_TOO_LARGE"); return bytes
    }
    fun hash(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
    fun digest(fields: Map<String,String>): String {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).use { d ->
            d.writeInt(fields.size)
            fields.toSortedMap().forEach { (k,v) -> for (s in listOf(k,v)) { val b=s.toByteArray(Charsets.UTF_8); d.writeInt(b.size); d.write(b) } }
        }
        return hash(out.toByteArray())
    }
    fun version(f: Map<String,String>) { demand(f["version"] == VERSION && f["contract"] == contract,"INCOMPATIBLE") }
    fun number(s: String, max: Long): Long { demand(s.matches(Regex("[1-9][0-9]{0,15}")),"INVALID_NUMBER"); val n=s.toLongOrNull() ?: throw Refusal("INVALID_NUMBER"); demand(n in 1..max,"INVALID_LIMIT"); return n }
}
