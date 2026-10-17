package com.stark.jarvislocal.identity

import android.util.JsonReader
import android.util.JsonToken
import java.io.StringReader
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest
import java.util.Collections

class IdentityRejected(val code: String) : IllegalArgumentException(code)
internal fun requireIdentity(ok: Boolean, code: String) { if (!ok) throw IdentityRejected(code) }

/** Strict Android API24 codec for the approved 001B canonical profile. No writes or authority. */
object IdentityCodec {
    const val VERSION = "jarvis.identity/1"
    const val REVISION = "001B.1-candidate" // Approved exact bytes; do not rename this field.
    const val BUNDLE = "431b5212f742bd9028477a6a603acf328c414801873efdd529f45b6ed5e619de"
    const val MAX_INTEGER = 9_007_199_254_740_991L
    private val keyPattern = Regex("[a-z][a-z0-9_]{0,47}")
    internal fun utf8(text: String): ByteArray = try {
        val b = Charsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).encode(CharBuffer.wrap(text))
        ByteArray(b.remaining()).also { b.get(it) }
    } catch (_: Exception) { throw IdentityRejected("INVALID_UTF8") }
    private fun text(s: String) {
        requireIdentity(utf8(s).size <= 8192 && s.none { it.code == 127 || it.code < 32 && it !in "\n\r\t" }, "STRING_LIMIT")
    }
    fun decode(bytes: ByteArray): Map<String, Any?> {
        requireIdentity(bytes.size <= 32768, "WIRE_LIMIT")
        val decoded = try { Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString() }
        catch (_: Exception) { throw IdentityRejected("INVALID_UTF8") }
        requireIdentity(!decoded.startsWith('\uFEFF'), "MALFORMED_JSON")
        // Android JsonReader strict mode still accepts raw LF/TAB within quoted strings.
        // Enforce the JSON lexical rule independently before it normalizes those characters.
        var quoted = false
        var escaped = false
        for (c in decoded) {
            if (quoted) {
                requireIdentity(c.code >= 32, "MALFORMED_JSON")
                if (escaped) escaped = false
                else if (c == '\\') escaped = true
                else if (c == '"') quoted = false
            } else if (c == '"') quoted = true
        }
        var nodes = 0
        fun read(r: JsonReader, depth: Int): Any? {
            requireIdentity(depth <= 8 && ++nodes <= 1024, "STRUCTURE_LIMIT")
            return when (r.peek()) {
                JsonToken.BEGIN_OBJECT -> {
                    val map = linkedMapOf<String, Any?>(); r.beginObject()
                    while (r.hasNext()) {
                        val k = r.nextName()
                        requireIdentity(keyPattern.matches(k), "INVALID_KEY")
                        requireIdentity(!map.containsKey(k), "DUPLICATE_FIELD")
                        map[k] = read(r, depth + 1)
                    }
                    r.endObject(); Collections.unmodifiableMap(map)
                }
                JsonToken.BEGIN_ARRAY -> {
                    val list = mutableListOf<Any?>(); r.beginArray()
                    while (r.hasNext()) { requireIdentity(list.size < 16, "LIST_LIMIT"); list += read(r, depth + 1) }
                    r.endArray(); Collections.unmodifiableList(list)
                }
                JsonToken.STRING -> r.nextString().also(::text)
                JsonToken.NUMBER -> {
                    val number = r.nextString()
                    requireIdentity(number.matches(Regex("0|[1-9][0-9]{0,15}")), "INTEGER_LIMIT")
                    val n = number.toLongOrNull() ?: throw IdentityRejected("INTEGER_LIMIT")
                    requireIdentity(n <= MAX_INTEGER, "INTEGER_LIMIT"); n
                }
                JsonToken.BOOLEAN -> r.nextBoolean()
                JsonToken.NULL -> { r.nextNull(); null }
                else -> throw IdentityRejected("MALFORMED_JSON")
            }
        }
        try {
            JsonReader(StringReader(decoded)).use { r ->
                r.isLenient = false
                val value = read(r, 0)
                requireIdentity(r.peek() == JsonToken.END_DOCUMENT, "TRAILING_DATA")
                requireIdentity(value is Map<*, *>, "OBJECT_REQUIRED")
                @Suppress("UNCHECKED_CAST")
                return value as Map<String, Any?>
            }
        } catch (e: IdentityRejected) { throw e }
        catch (_: Exception) { throw IdentityRejected("MALFORMED_JSON") }
    }
    fun canonical(value: Any?): ByteArray {
        var nodes = 0
        val out = StringBuilder()
        fun quote(s: String) {
            text(s); out.append('"')
            for (c in s) when (c) {
                '"' -> out.append("\\\""); '\\' -> out.append("\\\\")
                '\n' -> out.append("\\n"); '\r' -> out.append("\\r"); '\t' -> out.append("\\t")
                else -> out.append(c)
            }
            out.append('"')
        }
        fun emit(v: Any?, depth: Int) {
            requireIdentity(depth <= 8 && ++nodes <= 1024, "STRUCTURE_LIMIT")
            when (v) {
                null -> out.append("null")
                is String -> quote(v)
                is Boolean -> out.append(if (v) "true" else "false")
                is Int, is Long -> {
                    val n = (v as Number).toLong(); requireIdentity(n in 0..MAX_INTEGER, "INTEGER_LIMIT"); out.append(n)
                }
                is Map<*, *> -> {
                    requireIdentity(v.keys.all { it is String && keyPattern.matches(it) }, "INVALID_KEY")
                    out.append('{')
                    v.keys.map { it as String }.sorted().forEachIndexed { i, k ->
                        if (i > 0) out.append(','); quote(k); out.append(':'); emit(v[k], depth + 1)
                    }
                    out.append('}')
                }
                is List<*> -> {
                    requireIdentity(v.size <= 16, "LIST_LIMIT"); out.append('[')
                    v.forEachIndexed { i, item -> if (i > 0) out.append(','); emit(item, depth + 1) }; out.append(']')
                }
                else -> throw IdentityRejected("TYPE_NOT_SUPPORTED")
            }
        }
        emit(value, 0)
        val bytes = utf8(out.toString()); requireIdentity(bytes.size <= 32768, "WIRE_LIMIT"); return bytes
    }
    fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it.toInt() and 255) }
    fun digest(value: Any?): String = sha256("jarvis.identity/1:001B.1\u0000".toByteArray(Charsets.UTF_8) + canonical(value))
}
