package com.stark.jarvislocal.identity.profiles

import com.stark.jarvislocal.InferenceRequest
import com.stark.jarvislocal.identity.*

data class ProfileDifference(val field: String, val before: String, val after: String)
object DiffService {
    fun compare(before: ProfileDraft, after: ProfileDraft): List<ProfileDifference> {
        val changes = mutableListOf<ProfileDifference>()
        if (before.name != after.name) changes += ProfileDifference("name", before.name, after.name)
        if (before.preset != after.preset) changes += ProfileDifference("preset", before.preset.name, after.preset.name)
        for (context in PersonaContext.entries) {
            val a = before.parameters(context); val b = after.parameters(context)
            if (a != b) changes += ProfileDifference(context.name, a.toString(), b.toString())
        }
        if (before.interests != after.interests) changes += ProfileDifference("simulated_interests", before.interests.toString(), after.interests.toString())
        if (before.languageRules != after.languageRules) changes += ProfileDifference("language_rules", before.languageRules.toString(), after.languageRules.toString())
        return changes
    }
}
data class ProfileScenario(val id: String, val question: String, val context: PersonaContext)
data class ProfilePreview(val scenario: ProfileScenario, val parameters: PersonaParameters,
    val label: String = "Prévisualisation de configuration, aucune réponse LLM", val runtimeReady: Boolean = false)
class ProfilePreviewEngine {
    val scenarios = listOf(
        ProfileScenario("familiar", "Bonjour, comment organiser cette journée fictive ?", PersonaContext.DAILY),
        ProfileScenario("humour", "Une devinette synthétique, sans personne réelle.", PersonaContext.DAILY),
        ProfileScenario("fact", "Corriger une erreur factuelle de la fixture.", PersonaContext.WORK),
        ProfileScenario("technical", "Expliquer le test logiciel synthétique.", PersonaContext.WORK),
        ProfileScenario("adult", "Distinguer attirance et confiance entre adultes fictifs.", PersonaContext.DAILY),
        ProfileScenario("urgent", "Un risque sérieux dans une situation synthétique.", PersonaContext.URGENT))
    fun preview(profile: IdentityProfile): List<ProfilePreview> = scenarios.map { ProfilePreview(it, profile.draft.parameters(it.context)) }
    /** Same formatter/bytes as guarded IdentityComposer, baseline B only; this is not a permit. */
    fun technicalBaselineSystem(originalSystem: String, manifest: IdentityManifest): String {
        val fragment = IdentityStyleFormatter.render(manifest.styles)
        require(IdentityCodec.utf8(fragment).size <= 2048)
        return (originalSystem + "\n\n" + fragment).also { require(IdentityCodec.utf8(it).size <= 8192) }
    }
}

/** Does not convert editor percentages/profile names into the frozen B wire contract. */
class IdentityComposerAdapter(private val composer: IdentityComposer) {
    fun proposedProfileSupport(profile: IdentityProfile): String {
        require(profile.reference.revision >= 1)
        return "CONTRACT_EXTENSION_REQUIRED"
    }
    fun composeApprovedTechnicalBaseline(request: InferenceRequest, manifest: IdentityManifest,
        snapshot: IdentitySnapshot, separateSystemPrompt: Boolean): IdentityComposition =
        composer.compose(request, manifest, snapshot, separateSystemPrompt)
}

data class PersonaStyleIntent(val context: PersonaContext, val style: PersonaStyle, val tutoiement: Boolean)
/** Only expression labels: no profile name, free text, memory, authorizations or engine capability claim. */
fun interface PersonaExpressionPort { fun readStyle(context: PersonaContext): PersonaStyleIntent }
class ReadOnlyPersonaExpression(private val profile: IdentityProfile) : PersonaExpressionPort {
    override fun readStyle(context: PersonaContext) = profile.draft.parameters(context).let {
        PersonaStyleIntent(context, it.style, it.tutoiement)
    }
}
