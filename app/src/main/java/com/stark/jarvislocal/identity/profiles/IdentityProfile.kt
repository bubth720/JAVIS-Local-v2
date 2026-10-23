package com.stark.jarvislocal.identity.profiles

import java.util.Collections
import java.util.UUID
import com.stark.jarvislocal.identity.*

/** Editor-local proposal format; these percentages are NOT fields of jarvis.identity/1. */
enum class PersonaPreset { ATHENA, NEUTRAL }
enum class PersonaContext { DAILY, WORK, URGENT }
enum class PersonaStyle { ELEGANT, PLAIN }
data class PersonaParameters(val frankness: Int, val humour: Int, val initiative: Int,
    val tutoiement: Boolean, val style: PersonaStyle) {
    init { require(frankness in 0..100 && humour in 0..100 && initiative in 0..100) }
}
class ProfileDraft(val name: String, val preset: PersonaPreset, val defaults: PersonaParameters,
    overrides: Map<PersonaContext, PersonaParameters> = emptyMap(),
    interests: Set<PersonaInterest> = emptySet(), languageRules: List<LanguageRule> = emptyList()) {
    val overrides: Map<PersonaContext, PersonaParameters> = Collections.unmodifiableMap(LinkedHashMap(overrides))
    val interests: Set<PersonaInterest> = Collections.unmodifiableSet(LinkedHashSet(interests))
    val languageRules: List<LanguageRule> = Collections.unmodifiableList(ArrayList(languageRules))
    init { validateLanguageRules(this.languageRules); require(name.isNotBlank() && name.toByteArray(Charsets.UTF_8).size <= 120 && name.none { it.isISOControl() }) }
    fun parameters(context: PersonaContext): PersonaParameters = overrides[context] ?: defaults
    companion object {
        fun preset(preset: PersonaPreset) = when (preset) {
            PersonaPreset.ATHENA -> ProfileDraft("Athéna", preset, PersonaParameters(75, 60, 100, true, PersonaStyle.ELEGANT))
            PersonaPreset.NEUTRAL -> ProfileDraft("Neutre", preset, PersonaParameters(50, 0, 0, true, PersonaStyle.PLAIN))
        }
    }
}
data class ProfileReference(val id: String, val revision: Long, val sha256: String) {
    init { require(UUID.fromString(id).toString() == id && revision in 1..64 && sha256.matches(Regex("[0-9a-f]{64}"))) }
}
data class IdentityProfile(val reference: ProfileReference, val draft: ProfileDraft)
class ProfileRejected(val reason: String) : IllegalStateException(reason)

internal object ProfileCodec {
    private fun params(p: PersonaParameters): Map<String, Any> = mapOf("frankness" to p.frankness.toLong(),
        "humour" to p.humour.toLong(), "initiative" to p.initiative.toLong(), "tutoiement" to p.tutoiement, "style" to p.style.name)
    fun encode(id: String, revision: Long, p: ProfileDraft): ByteArray = IdentityCodec.canonical(mapOf(
        "format" to "jarvis.identity-profile-lab/2", "id" to id, "revision" to revision,
        "name" to p.name, "preset" to p.preset.name, "defaults" to params(p.defaults),
        "interests" to p.interests.map { it.name }.sorted(),
        "language_rules" to p.languageRules.map { mapOf("source" to it.source.name, "target" to it.target.name) },
        "overrides" to p.overrides.mapKeys { it.key.name.lowercase(java.util.Locale.ROOT) }.mapValues { params(it.value) }))
    fun digest(bytes: ByteArray): String = IdentityCodec.digest(IdentityCodec.decode(bytes))
    private fun parseParams(value: Any?): PersonaParameters {
        val map = value as? Map<*, *> ?: throw ProfileRejected("INVALID_PARAMETERS")
        require(map.keys == setOf("frankness", "humour", "initiative", "tutoiement", "style"))
        fun number(key: String) = (map[key] as? Long)?.also { require(it in 0..100) }?.toInt() ?: throw ProfileRejected("INVALID_PARAMETER")
        return PersonaParameters(number("frankness"), number("humour"), number("initiative"),
            map["tutoiement"] as? Boolean ?: throw ProfileRejected("INVALID_PARAMETER"),
            PersonaStyle.valueOf(map["style"] as? String ?: throw ProfileRejected("INVALID_PARAMETER")))
    }
    fun decode(bytes: ByteArray, expected: ProfileReference): IdentityProfile {
        require(bytes.size <= 32768 && digest(bytes) == expected.sha256)
        val map = IdentityCodec.decode(bytes)
        val legacy = map["format"] == "jarvis.identity-profile-lab/1"
        val keys = setOf("format", "id", "revision", "name", "preset", "defaults", "overrides")
        require(map.keys == if (legacy) keys else keys + setOf("interests", "language_rules"))
        require((legacy || map["format"] == "jarvis.identity-profile-lab/2") && map["id"] == expected.id && map["revision"] == expected.revision)
        val interests = if (legacy) emptySet() else {
            val values = map["interests"] as? List<*> ?: throw ProfileRejected("INVALID_INTERESTS")
            require(values.size <= PersonaInterest.entries.size && values.distinct().size == values.size)
            values.map { PersonaInterest.valueOf(it as? String ?: throw ProfileRejected("INVALID_INTEREST")) }.toSet()
        }
        val rules = if (legacy) emptyList() else {
            val values = map["language_rules"] as? List<*> ?: throw ProfileRejected("INVALID_RULES")
            require(values.size <= 4)
            values.map {
                val rule = it as? Map<*, *> ?: throw ProfileRejected("INVALID_RULE")
                require(rule.keys == setOf("source", "target"))
                LanguageRule(LexicalToken.valueOf(rule["source"] as? String ?: throw ProfileRejected("INVALID_RULE")),
                    LexicalToken.valueOf(rule["target"] as? String ?: throw ProfileRejected("INVALID_RULE")))
            }
        }
        val overrides = map["overrides"] as? Map<*, *> ?: throw ProfileRejected("INVALID_PARAMETERS")
        require(overrides.size <= 3)
        return IdentityProfile(expected, ProfileDraft(map["name"] as? String ?: throw ProfileRejected("INVALID_NAME"),
            PersonaPreset.valueOf(map["preset"] as? String ?: throw ProfileRejected("INVALID_PRESET")),
            parseParams(map["defaults"]), overrides.entries.associate {
                PersonaContext.valueOf((it.key as? String ?: throw ProfileRejected("INVALID_CONTEXT")).uppercase(java.util.Locale.ROOT)) to parseParams(it.value)
            }, interests, rules))
    }
}
