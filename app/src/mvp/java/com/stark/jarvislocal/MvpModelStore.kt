package com.stark.jarvislocal

import android.util.AtomicFile
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

internal data class MvpModelManifest(val provenance: String, val license: String, val sha256: String, val bytes: Long) {
    fun validate(policy: MvpImportPolicy) {
        require(provenance.isNotBlank() && provenance.length <= 512) { "Provenance publique requise (512 caractères maximum)" }
        require(license.isNotBlank() && license.length <= 256) { "Licence requise (256 caractères maximum)" }
        require(sha256.matches(Regex("[a-f0-9]{64}"))) { "SHA-256 attendu requis : 64 caractères hexadécimaux minuscules" }
        require(bytes > policy.minimum && bytes <= policy.maximum) { "Taille autorisée : plus de 100 Mio et au plus 512 Mio" }
    }
}
internal data class MvpImportPolicy(val minimum: Long = 100L * 1024 * 1024,
    val maximum: Long = 512L * 1024 * 1024, val reserve: Long = 256L * 1024 * 1024)

/** Separate, private MVP store. No asset, legacy model, SQLite or production preference mutation. */
internal class MvpModelStore(private val root: File, private val policy: MvpImportPolicy = MvpImportPolicy(),
    private val freeBytes: () -> Long = { root.usableSpace }) {
    private val metadata get() = AtomicFile(File(root, "selected.json"))
    private fun path(m: MvpModelManifest) = File(root, m.sha256 + ".gguf")
    @Synchronized fun selected(): MvpModelManifest? {
        if (!root.exists()) return null
        return try {
            val j = JSONObject(metadata.openRead().bufferedReader().use { it.readText() })
            require(j.getInt("version") == 1) { "Version de manifeste inconnue" }
            MvpModelManifest(j.getString("provenance"), j.getString("license"), j.getString("sha256"), j.getLong("bytes"))
                .also { it.validate(policy); require(quickValid(it)) { "Modèle sélectionné incomplet" } }
        } catch (missing: java.io.FileNotFoundException) { null }
    }
    @Synchronized fun selectedFile(): File = path(requireNotNull(selected()) { "Importer un GGUF vérifié avant le chargement" })
    @Synchronized fun ready(): Boolean = try { selected()?.let(::quickValid) == true } catch (_: Exception) { false }
    private fun quickValid(m: MvpModelManifest): Boolean = path(m).isFile && path(m).length() == m.bytes
    @Synchronized fun verifySelected(checkCancelled: () -> Unit = {}) {
        val m = requireNotNull(selected()) { "Modèle absent" }
        verify(path(m), m, checkCancelled)
    }

    @Synchronized fun install(m: MvpModelManifest, open: () -> InputStream, checkCancelled: () -> Unit) {
        m.validate(policy)
        check(root.isDirectory || root.mkdirs()) { "Stockage privé indisponible" }
        val free = freeBytes()
        require(free >= m.bytes && free - m.bytes >= policy.reserve) { "Espace disque insuffisant (réserve 256 Mio)" }
        val part = File(root, "import.part")
        check(!part.exists() || part.delete()) { "Copie précédente à nettoyer ; import bloqué" }
        var primary: Throwable? = null
        try {
            checkCancelled()
            open().use { input -> FileOutputStream(part).use { output ->
                val buffer = ByteArray(1024 * 1024)
                var remaining = m.bytes
                try {
                    while (remaining > 0) {
                        checkCancelled()
                        val n = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                        require(n > 0) { "Modèle tronqué ou lecture interrompue" }
                        output.write(buffer, 0, n)
                        remaining -= n
                    }
                    checkCancelled()
                    require(input.read() == -1) { "Source plus grande que la taille annoncée" }
                    output.fd.sync()
                } finally { buffer.fill(0) }
            } }
            verify(part, m, checkCancelled)
            checkCancelled()
            val destination = path(m)
            if (destination.exists()) verify(destination, m, checkCancelled)
            else check(part.renameTo(destination)) { "Installation atomique impossible" }
            checkCancelled()
            val json = JSONObject().put("version", 1).put("provenance", m.provenance).put("license", m.license)
                .put("sha256", m.sha256).put("bytes", m.bytes).toString().toByteArray(Charsets.UTF_8)
            val atomic = metadata
            val output = atomic.startWrite()
            try {
                output.write(json)
                output.fd.sync()
                checkCancelled()
                atomic.finishWrite(output)
            } catch (failure: Throwable) { atomic.failWrite(output); throw failure }
        } catch (failure: Throwable) { primary = failure; throw failure }
        finally {
            if (part.exists() && !part.delete()) {
                val cleanup = java.io.IOException("Nettoyage temporaire échoué ; nouvel import bloqué tant que le temporaire subsiste")
                if (primary == null) throw cleanup else primary.addSuppressed(cleanup)
            }
        }
    }
    private fun verify(file: File, m: MvpModelManifest, checkCancelled: () -> Unit) {
        require(file.length() == m.bytes) { "Taille GGUF incorrecte" }
        file.inputStream().use { input ->
            val header = ByteArray(24)
            var position = 0
            while (position < header.size) {
                checkCancelled()
                val n = input.read(header, position, header.size - position)
                require(n > 0) { "En-tête GGUF tronqué" }; position += n
            }
            require(header.copyOfRange(0, 4).contentEquals(byteArrayOf(0x47,0x47,0x55,0x46))) { "Signature GGUF absente" }
            val h = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
            val version = h.getInt(4); val tensors = h.getLong(8); val values = h.getLong(16)
            require(version in 2..3 && tensors in 1..1_000_000 && values in 1..1_000_000) { "En-tête GGUF non pris en charge" }
            val digest = MessageDigest.getInstance("SHA-256"); digest.update(header)
            val buffer = ByteArray(1024 * 1024)
            try {
                while (true) {
                    checkCancelled(); val n = input.read(buffer)
                    if (n < 0) break
                    require(n > 0) { "Lecture de vérification interrompue" }; digest.update(buffer, 0, n)
                }
            } finally { buffer.fill(0) }
            val actual = digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
            require(actual == m.sha256) { "SHA-256 différent de l'empreinte attendue" }
        }
    }
}
