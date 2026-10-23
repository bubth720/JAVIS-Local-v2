package com.stark.jarvislocal.identity

import com.stark.jarvislocal.identity.profiles.*

data class ContextStyleResolution(val parameters: PersonaParameters, val explanations: List<String>)
/** Editorial context only; this never grants an action, activates SYSTEM or calls an engine. */
object ContextStylePolicy {
    fun resolve(draft: ProfileDraft, context: PersonaContext): ContextStyleResolution {
        val requested = draft.parameters(context)
        return when (context) {
            PersonaContext.DAILY -> ContextStyleResolution(requested, listOf("CONFIGURATION_ONLY", "DAILY_STYLE"))
            PersonaContext.WORK -> ContextStyleResolution(requested.copy(humour = minOf(requested.humour, 30)),
                listOf("CONFIGURATION_ONLY", "WORK_FACTUAL_CLARITY"))
            PersonaContext.URGENT -> ContextStyleResolution(requested.copy(humour = 0, initiative = minOf(requested.initiative, 40)),
                listOf("CONFIGURATION_ONLY", "URGENT_NO_HUMOUR", "URGENT_LEXICAL_RULES_SUSPENDED"))
        }
    }
}
