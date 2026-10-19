package com.stark.jarvislocal

import android.app.Application
import android.util.AtomicFile
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Small synthetic headers ONLY, not executable GGUF models. Production thresholds unchanged. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[24,25,35], application=Application::class)
class MvpModelStoreTest {
    @get:Rule val folder = TemporaryFolder()
    private val policy = MvpImportPolicy(24, 4096, 32)
    private val root get() = File(folder.root, "store")
    private fun store(free: Long = 100000) = MvpModelStore(root, policy) { free }
    private fun bytes(seed: Int = 0): ByteArray = ByteArray(128) { seed.toByte() }.also {
        ByteBuffer.wrap(it).order(ByteOrder.LITTLE_ENDIAN).put(byteArrayOf(0x47,0x47,0x55,0x46)).putInt(3).putLong(1).putLong(1)
    }
    private fun manifest(b: ByteArray) = MvpModelManifest("public synthetic fixture", "CC0 test data", MessageDigest.getInstance("SHA-256")
        .digest(b).joinToString("") { "%02x".format(it.toInt() and 255) }, b.size.toLong())
    private fun install(s: MvpModelStore, b: ByteArray) = s.install(manifest(b), { ByteArrayInputStream(b) }, {})
    private fun refused(block: () -> Unit) { try { block(); fail("Refusal expected") } catch (_: IllegalArgumentException) { } }
    @Test fun importAndRestartKeepProvenanceAndBytes() {
        val b=bytes(); install(store(),b)
        val reopened=store(); assertEquals(manifest(b),reopened.selected()); reopened.verifySelected()
        assertArrayEquals(b,reopened.selectedFile().readBytes())
        assertFalse(File(root,"import.part").exists())
    }
    @Test fun wrongHashDoesNotReplacePreviousModel() {
        val b=bytes(); install(store(),b)
        refused { store().install(manifest(bytes(1)).copy(sha256="0".repeat(64)),{ByteArrayInputStream(bytes(1))},{}) }
        assertEquals(manifest(b),store().selected()); assertArrayEquals(b,store().selectedFile().readBytes())
    }
    @Test fun nonGgufRefusedEvenWithMatchingHash() {
        val b=ByteArray(128); refused { install(store(),b) }; assertNull(store().selected())
    }
    @Test fun unsupportedVersionRefused() {
        val b=bytes(); ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).putInt(4,99)
        refused { install(store(),b) }
    }
    @Test fun truncatedAndGrowingSourcesRefused() {
        val b=bytes(); val m=manifest(b)
        for (source in listOf(b.copyOf(100), b+byteArrayOf(1))) {
            refused { store().install(m,{ByteArrayInputStream(source)},{}) }
            assertNull(store().selected()); assertFalse(File(root,"import.part").exists())
        }
    }
    @Test fun declarationsAndBoundsValidatedBeforeOpening() {
        var opened=false; val m=manifest(bytes())
        for (bad in listOf(m.copy(license=""),m.copy(provenance=""),m.copy(sha256="x"),m.copy(bytes=24),m.copy(bytes=4097))) {
            refused { store().install(bad,{opened=true;ByteArrayInputStream(bytes())},{}) }
        }
        assertFalse(opened)
        refused { m.copy(bytes=100L*1024*1024).validate(MvpImportPolicy()) }
    }
    @Test fun spaceReserveRefusedWithoutOpeningProvider() {
        var opened=false
        refused { store(159).install(manifest(bytes()),{opened=true;ByteArrayInputStream(bytes())},{}) }
        assertFalse(opened)
    }
    @Test fun cancellationPreservesPreviousAndAllowsRetry() {
        val b=bytes(); install(store(),b); var checks=0
        try {
            store().install(manifest(bytes(1)),{ByteArrayInputStream(bytes(1))},{ if (++checks==3) throw java.util.concurrent.CancellationException() })
            fail("cancel expected")
        } catch (_:java.util.concurrent.CancellationException) { }
        assertEquals(manifest(b),store().selected()); assertFalse(File(root,"import.part").exists())
        install(store(),bytes(1)); assertEquals(manifest(bytes(1)),store().selected())
        assertArrayEquals(b,File(root,manifest(b).sha256+".gguf").readBytes())
    }
    @Test fun readFailureDoesNotLoseSelectedModel() {
        val b=bytes(); install(store(),b)
        try { store().install(manifest(bytes(1)),{object:java.io.InputStream(){override fun read():Int=throw IOException("synthetic")}},{});fail() }
        catch (_:IOException) { }
        assertEquals(manifest(b),store().selected())
    }
    @Test fun interruptedPartIsNeverAppendedAndRestartDoesNotAutoLoad() {
        root.mkdirs();File(root,"import.part").writeText("interrupted")
        assertNull(store().selected());install(store(),bytes());store().verifySelected()
    }
    @Test fun interruptedMetadataCommitRecoversPreviousSelection() {
        val b=bytes();install(store(),b)
        AtomicFile(File(root,"selected.json")).startWrite().use { it.write("partial".toByteArray()) }
        assertEquals(manifest(b),store().selected());store().verifySelected()
    }
    @Test fun corruptInstalledFileIsRejectedOnExplicitRevalidation() {
        install(store(),bytes());val path=store().selectedFile();path.writeBytes(bytes(3))
        refused { store().verifySelected() }
    }
    @Test fun interruptionAfterAtomicModelRenameRetainsOldSelectionAndCanResume() {
        val old=bytes();install(store(),old);val next=bytes(7);val manifest=manifest(next)
        try {
            store().install(manifest,{next.inputStream()},{
                if (File(root,manifest.sha256+".gguf").exists()) throw java.util.concurrent.CancellationException()
            });fail("cancel expected")
        } catch (_:java.util.concurrent.CancellationException) { }
        assertEquals(manifest(old),store().selected())
        assertArrayEquals(next,File(root,manifest.sha256+".gguf").readBytes())
        install(store(),next);assertEquals(manifest,store().selected());store().verifySelected()
    }

}
