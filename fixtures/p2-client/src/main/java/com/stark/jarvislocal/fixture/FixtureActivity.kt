package com.stark.jarvislocal.fixture

import android.app.Activity
import android.content.*
import android.os.*
import android.widget.*
import fr.jarvis.fixture.p2.IP2Endpoint
import java.util.UUID
import java.util.concurrent.Executors

/** Independent synthetic fixture. No historical Application, model warmup or private data. */
class FixtureActivity : Activity(), ServiceConnection {
    private val worker=Executors.newSingleThreadExecutor()
    private lateinit var status: TextView
    private val liveness=Binder()
    private val session=UUID.randomUUID().toString()
    private var bound=false
    private var closed=false
    private val death=IBinder.DeathRecipient { lost() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        status=TextView(this).apply { setText(R.string.pilot_initial) }
        setContentView(LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL; setPadding(32,32,32,32); addView(status)
            addView(Button(this@FixtureActivity).apply {
                setText(R.string.pilot_inspect)
                setOnClickListener { if (!bound) inspectAndBind() }
            })
        })
    }
    private fun inspectAndBind() {
        try {
            val own=AndroidPeerResolver(packageManager).resolve(android.os.Process.myUid())
            requirePilot(own.certificateDigests.size == 1,"FIXTURE_CERTIFICATE_REQUIRED")
            val pin=PublishedEndpointPin(ComponentName(Wire.ENDPOINT,"${Wire.ENDPOINT}.P2EndpointService"),own.certificateDigests.single(),
                Wire.PERMISSION,"5751646acd4a7bb13caaea4b96a4ab3a68ec6adb",Wire.VERSION,Wire.contract)
            val readiness=EndpointInspector(this).inspect(pin)
            requirePilot(readiness == EndpointReadiness.IDENTITY_VERIFIED,readiness.name)
            bound=bindService(Intent().setComponent(pin.component),this,Context.BIND_AUTO_CREATE)
            requirePilot(bound,"BIND_REFUSED")
        } catch (e: Exception) { status.text=getString(R.string.pilot_refused,(e as? PilotBlocked)?.code ?: "IDENTITY_OR_SERVICE_ERROR") }
    }
    override fun onServiceConnected(name: ComponentName,binder: IBinder) {
        try {
            requirePilot(name == ComponentName(Wire.ENDPOINT,"${Wire.ENDPOINT}.P2EndpointService"),"WRONG_COMPONENT")
            binder.linkToDeath(death,0)
            worker.execute {
                try {
                    val reply=IP2Endpoint.Stub.asInterface(binder).hello(Wire.encode(mapOf("version" to Wire.VERSION,"contract" to Wire.contract,"session_id" to session)),liveness)
                    val names=setOf("version","contract","epoch","session_id","synthetic_text_only","canonical_native_p2","interprocess_authority","global_android_arbiter","android_interprocess_e2e","hardware_qualified","model_import_supported")
                    val fields=Wire.decode(reply,names); Wire.version(fields)
                    requirePilot(fields["session_id"] == session && canonicalUuid(fields.getValue("epoch")) && fields["synthetic_text_only"] == "true" &&
                        fields["canonical_native_p2"] == "true" && fields["interprocess_authority"] == "required_external_core","P2_CAPABILITY_REQUIRED")
                    show("Hello Binder corrélé reçu. Ce résultat ne prouve pas une génération native.\nAdmission bloquée : aucun reçu initial de libération/réconciliation disponible dans 003E.")
                } catch (_: Exception) { lost(); show("Handshake refusé ou liaison perdue. Aucun chargement autorisé.") }
            }
        } catch (_: Exception) { lost() }
    }
    private fun show(message: String) { runOnUiThread { if (!closed) status.text=message } }
    private fun lost() { runCatching { PilotRuntime.get(this).arbiter.connectionLost() } }
    override fun onServiceDisconnected(name: ComponentName) { lost() }
    override fun onBindingDied(name: ComponentName) { lost() }
    override fun onNullBinding(name: ComponentName) { lost() }
    override fun onDestroy() {
        closed=true
        if (bound) { unbindService(this); bound=false; lost() }
        worker.shutdown() // Not an assertion that a synchronous Binder/native call has ended.
        super.onDestroy()
    }
}
