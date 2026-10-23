package com.stark.jarvislocal.identity

import com.stark.jarvislocal.identity.profiles.*

/** Finite vocabulary for trusted demonstration templates, never instructions or generated answers. */
enum class LexicalToken(val text: String) { BONJOUR("Bonjour"), SALUT("Salut"), MERCI("Merci"), VOLONTIERS("Volontiers") }
data class LanguageRule(val source: LexicalToken, val target: LexicalToken) {
    init { require(source != target) { "REDUNDANT_RULE" } }
}
internal fun validateLanguageRules(rules: List<LanguageRule>) {
    require(rules.size <= 4 && rules.map { it.source }.distinct().size == rules.size) { "CONFLICTING_LANGUAGE_RULES" }
    require(rules.none { rule -> rules.any { it.source == rule.target } }) { "CHAINED_LANGUAGE_RULES" }
}
data class AppliedLanguageRule(val source: LexicalToken, val target: LexicalToken, val start: Int, val endExclusive: Int)
data class LanguagePreview(val context: PersonaContext, val before: String, val after: String,
    val rules: List<AppliedLanguageRule>, val effective: PersonaParameters, val explanations: List<String>,
    val label: String = "Gabarit local de configuration — aucune réponse LLM ni permission")

/** No free-text input: neither documents, citations nor model output are rewritten. */
class TemplateRealizer {
    fun preview(draft: ProfileDraft, context: PersonaContext): LanguagePreview {
        validateLanguageRules(draft.languageRules)
        val policy = ContextStylePolicy.resolve(draft, context)
        val before = when (context) {
            PersonaContext.DAILY -> "Bonjour. Merci pour cette question. Explorons cette idée ensemble."
            PersonaContext.WORK -> "Bonjour. Merci pour ces éléments. Vérifions les faits avant de conclure."
            PersonaContext.URGENT -> "Bonjour. Concentrons-nous sur le risque immédiat et les étapes utiles."
        }
        val rules = draft.languageRules.associateBy { it.source.text }
        val changes = mutableListOf<AppliedLanguageRule>()
        val expression = Regex("(?<![\\p{L}\\p{N}_])(?:Bonjour|Salut|Merci|Volontiers)(?![\\p{L}\\p{N}_])")
        val after = expression.replace(before) { match ->
            val rule = rules[match.value]
            if (rule == null || context == PersonaContext.URGENT) match.value
            else {
                changes += AppliedLanguageRule(rule.source, rule.target, match.range.first, match.range.last + 1)
                rule.target.text
            }
        }
        return LanguagePreview(context, before, after, changes.toList(), policy.parameters, policy.explanations)
    }
}
