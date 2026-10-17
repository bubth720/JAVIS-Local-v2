package com.stark.jarvislocal.fixture

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import java.security.MessageDigest

internal data class PeerFacts(val uid: Int, val packages: Set<String>, val certificateDigests: Set<String>)
internal data class PeerPin(val uid: Int, val packageName: String, val certificateSha256: String)
internal fun requirePeer(facts: PeerFacts, pin: PeerPin) {
    requirePilot(pin.uid >= 0 && pin.certificateSha256.matches(Regex("[0-9a-f]{64}")), "INVALID_PEER_PIN")
    requirePilot(facts.uid == pin.uid && facts.packages == setOf(pin.packageName) &&
        facts.certificateDigests == setOf(pin.certificateSha256), "BINDER_IDENTITY_REFUSED")
}

/** Must be called inside a Binder entry transaction BEFORE asynchronous dispatch. */
internal class BinderCallerGuard(private val resolve: (Int) -> PeerFacts) {
    fun capture(pin: PeerPin): PeerFacts {
        val uid = Binder.getCallingUid()
        return resolve(uid).also { requirePeer(it, pin) }
    }
}

internal class AndroidPeerResolver(private val pm: PackageManager) {
    fun resolve(uid: Int): PeerFacts {
        val packages = pm.getPackagesForUid(uid)?.toSet().orEmpty()
        val certificates = packages.flatMap { name ->
            val info = pm.getPackageInfo(name, PackageManager.GET_SIGNING_CERTIFICATES)
            val signing = info.signingInfo
            requirePilot(signing != null && !signing.hasMultipleSigners() && signing.signingCertificateHistory?.size == 1, "SIGNING_ROTATION_REFUSED")
            signing?.apkContentsSigners.orEmpty().map {
                MessageDigest.getInstance("SHA-256").digest(it.toByteArray()).joinToString("") { byte -> "%02x".format(byte.toInt() and 255) }
            }
        }.toSet()
        return PeerFacts(uid, packages, certificates)
    }
}

internal data class PublishedEndpointPin(val component: ComponentName, val certificateSha256: String,
    val requiredSignaturePermission: String, val sourceCommit: String, val contractVersion: String, val contractSha256: String)
internal enum class EndpointReadiness { PUBLICATION_REQUIRED, UNSUPPORTED_DEVICE, SERVICE_ABSENT, IDENTITY_VERIFIED }

/** Identity inspection before binding; this alone is never a native capability proof. */
internal class EndpointInspector(private val context: Context) {
    fun inspect(pin: PublishedEndpointPin?): EndpointReadiness {
        if (pin == null) return EndpointReadiness.PUBLICATION_REQUIRED
        if ("arm64-v8a" !in Build.SUPPORTED_ABIS) return EndpointReadiness.UNSUPPORTED_DEVICE
        requirePilot(pin.sourceCommit.matches(Regex("[0-9a-f]{40}")) && pin.contractSha256.matches(Regex("[0-9a-f]{64}")) &&
            pin.contractVersion.isNotEmpty(), "UNPINNED_CONTRACT")
        val pm = context.packageManager
        val info = pm.resolveService(Intent().setComponent(pin.component), 0)?.serviceInfo ?: return EndpointReadiness.SERVICE_ABSENT
        requirePilot(info.packageName == pin.component.packageName && info.name == pin.component.className &&
            info.enabled && info.exported && info.applicationInfo.enabled, "INVALID_SERVICE")
        requirePilot(info.permission == pin.requiredSignaturePermission && pin.requiredSignaturePermission.isNotEmpty(), "SIGNATURE_PERMISSION_REQUIRED")
        val permission = pm.getPermissionInfo(pin.requiredSignaturePermission, 0)
        requirePilot(permission.packageName == pin.component.packageName && permission.protection ==
            android.content.pm.PermissionInfo.PROTECTION_SIGNATURE, "SIGNATURE_PERMISSION_REQUIRED")
        requirePilot(pm.checkPermission(pin.requiredSignaturePermission, context.packageName) == PackageManager.PERMISSION_GRANTED, "CLIENT_NOT_ENROLLED")
        requirePeer(AndroidPeerResolver(pm).resolve(info.applicationInfo.uid),
            PeerPin(info.applicationInfo.uid, pin.component.packageName, pin.certificateSha256))
        return EndpointReadiness.IDENTITY_VERIFIED
    }
}
