package com.stark.jarvislocal.identity

import com.stark.jarvislocal.identity.profiles.*
import com.stark.jarvislocal.memory.*

enum class PersonaInterest { HISTORY, MYTHOLOGY, PHILOSOPHY, SCIENCE, STRATEGY, ARTS, LITERATURE }
data class SimulatedCharacterTaste(val interest: PersonaInterest, val simulated: Boolean = true)
data class ConfirmedUserPreference(val reference: MemoryReference, val untrustedContent: String)
fun interface ConfirmedPreferencePort { fun read(limit: Int): List<ConfirmedUserPreference> }
/** Read-only view over the existing consent store. No inference from chats/documents and no write API. */
class MemoryPreferenceReader(private val lookup: MemoryLookupPort) : ConfirmedPreferencePort {
    override fun read(limit: Int): List<ConfirmedUserPreference> {
        require(limit in 1..10)
        return lookup.recent(ProjectionPolicy(setOf(MemoryScope.PREFERENCE)), limit)
            .filter { it.scope == MemoryScope.PREFERENCE }.take(limit)
            .map { ConfirmedUserPreference(it.reference, it.content) }
    }
}
data class PreferencesView(val simulatedTastes: List<SimulatedCharacterTaste>,
    val editorStyle: PersonaParameters, val confirmedUserPreferences: List<ConfirmedUserPreference>,
    val programmedIdentityContract: String = "jarvis.identity/1 — unchanged; editor activation unsupported")
class PreferencesCatalog(private val confirmed: ConfirmedPreferencePort = ConfirmedPreferencePort { emptyList() }) {
    fun read(draft: ProfileDraft, context: PersonaContext): PreferencesView = PreferencesView(
        draft.interests.sortedBy { it.name }.map { SimulatedCharacterTaste(it) },
        ContextStylePolicy.resolve(draft, context).parameters, confirmed.read(10))
}
