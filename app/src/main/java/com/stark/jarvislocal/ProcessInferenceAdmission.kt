package com.stark.jarvislocal

/** In-process admission only. A retained owner survives controller replacement and GC. */
internal class ProcessInferenceAdmission {
    private var owner: Any? = null
    private var quarantined = false
    private var generation = 0L

    @Synchronized fun acquire(backend: Any): Long {
        if (owner != null && owner !== backend) throw InferenceRejected(InferenceRefusal.PROCESS_ENGINE_HELD)
        if (quarantined) throw InferenceRejected(InferenceRefusal.RELEASE_UNCONFIRMED)
        if (owner == null) generation++
        owner = backend
        return generation
    }

    @Synchronized fun confirmRelease(backend: Any, token: Long) {
        // A stale confirmation must never free a newer owner's reservation.
        if (owner === backend && token == generation) {
            if (quarantined) throw InferenceRejected(InferenceRefusal.RELEASE_UNCONFIRMED)
            owner = null
        }
    }

    @Synchronized fun quarantine(backend: Any, token: Long) {
        if (owner === backend && token == generation) quarantined = true
    }

    companion object {
        val process = ProcessInferenceAdmission()
    }
}
