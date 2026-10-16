package com.stark.jarvislocal

/** In-process admission only. A retained owner survives controller replacement and GC. */
internal class ProcessInferenceAdmission {
    private var owner: Any? = null
    private var quarantined = false

    @Synchronized fun acquire(backend: Any) {
        if (owner != null && owner !== backend) throw InferenceRejected(InferenceRefusal.PROCESS_ENGINE_HELD)
        if (quarantined) throw InferenceRejected(InferenceRefusal.RELEASE_UNCONFIRMED)
        owner = backend
    }

    @Synchronized fun confirmRelease(backend: Any) {
        // A stale confirmation must never free a newer owner's reservation.
        if (owner === backend) {
            if (quarantined) throw InferenceRejected(InferenceRefusal.RELEASE_UNCONFIRMED)
            owner = null
        }
    }

    @Synchronized fun quarantine(backend: Any) {
        if (owner === backend) quarantined = true
    }

    companion object {
        val process = ProcessInferenceAdmission()
    }
}
