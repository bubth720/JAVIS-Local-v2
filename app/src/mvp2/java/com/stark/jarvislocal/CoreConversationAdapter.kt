package com.stark.jarvislocal

/** Real CORE adapter. No mock result, automatic memory, migration or external module. */
internal class CoreConversationAdapter(private val database: LocalDatabase, private val settings: SettingsStore,
    private val core: JarvisCore) : MvpConversation {
    override fun recent(): List<ChatMessage> = database.recentMessages(40)
    override fun needsModel(text: String): Boolean =
        !(text.trim().startsWith("/remember ", true) || text.trim().equals("/docs", true))
    override suspend fun chat(text: String, settings: JarvisSettings): ChatMessage {
        this.settings.save(settings.copy(speakReplies = false)) // speech is explicit via the UI adapter only
        return core.chat(text)
    }
}

internal enum class FutureCapability { NEXUS, SERVICES, EVOLUTION }
internal data class DisabledExtension(val capability: FutureCapability, val available: Boolean = false)
internal val disabledExtensions = FutureCapability.entries.map { DisabledExtension(it) }
