package com.stark.jarvislocal.fixture

import com.stark.jarvislocal.InferenceSource
import org.junit.Assert.*
import org.junit.Test

class PilotRequestGateTest {
    @Test fun systemAndQuestionRemainSeparateBeforeTransport() {
        val r=request(); PilotRequestGate.validate(r)
        assertFalse(r.question.contains(r.systemPrompt))
    }
    @Test fun allContextIncludingWhitespaceAndAnySourceIsRefused() {
        for (context in listOf(" ","MEMOIRE privée","[SOURCE 1] document","HISTORIQUE:"))
            assertThrows(PilotBlocked::class.java) { PilotRequestGate.validate(request().copy(context=context)) }
        assertThrows(PilotBlocked::class.java) { PilotRequestGate.validate(request().copy(sources=listOf(InferenceSource(1,2,"private.pdf",0)))) }
    }
    @Test fun invalidIdsUtf8LimitsAndOversizedInputsAreRefused() {
        for (r in listOf(request().copy(sessionId="legacy"),request().copy(question="\uD800"),
            request().copy(systemPrompt="x".repeat(8193)),request().copy(contractVersion=99)))
            assertThrows(PilotBlocked::class.java) { PilotRequestGate.validate(r) }
    }
}
