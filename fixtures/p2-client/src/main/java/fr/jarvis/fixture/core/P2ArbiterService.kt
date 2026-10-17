package fr.jarvis.fixture.core

import android.app.Service
import android.content.Intent
import android.os.*
import com.stark.jarvislocal.fixture.*
import fr.jarvis.fixture.p2.IP2Arbiter
import fr.jarvis.fixture.p2.IP2AuthorizationCallback

/** Fixture only. Authenticate before AIDL unmarshalling and before processing a query. */
class P2ArbiterService : Service() {
    private fun caller(): Int {
        val uid=Binder.getCallingUid()
        val pm=packageManager
        val own=AndroidPeerResolver(pm).resolve(Process.myUid())
        requirePilot(own.packages == setOf(Wire.CLIENT) && own.certificateDigests.size == 1,"INVALID_FIXTURE_SIGNER")
        val peer=pm.getApplicationInfo(Wire.ENDPOINT,0)
        requirePeer(AndroidPeerResolver(pm).resolve(uid),PeerPin(peer.uid,Wire.ENDPOINT,own.certificateDigests.single()))
        return uid
    }
    private val endpoint=object : IP2Arbiter.Stub() {
        override fun onTransact(code: Int,data: Parcel,reply: Parcel?,flags: Int): Boolean {
            if (code in IBinder.FIRST_CALL_TRANSACTION..IBinder.LAST_CALL_TRANSACTION) {
                try { caller(); requirePilot(data.dataSize() <= 32768,"INPUT_TOO_LARGE") }
                catch (_: Exception) { throw SecurityException("P2 authority refused") }
            }
            return super.onTransact(code,data,reply,flags)
        }
        override fun authorize(query: ByteArray,callback: IP2AuthorizationCallback) {
            try {
                val uid=caller()
                val names=Wire.keyNames + setOf("request_digest","model","model_sha256","profile","context_tokens","output_tokens","remaining_ms","client_uid","server_uid")
                val fields=Wire.decode(query,names)
                val result=PilotRuntime.get(this@P2ArbiterService).grants.approve(fields,uid,Process.myUid())
                // Remote callback originates from this process, never a fabricated body identity.
                callback.complete(Wire.encode(result))
            } catch (_: Exception) {
                // No positive acknowledgement on failure. Timeout retains the reservation.
            }
        }
    }
    override fun onBind(intent: Intent): IBinder = endpoint
}
