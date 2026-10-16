package com.stark.jarvislocal

import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ModelInstallationTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun bytes(path: String, size: Int = 128) = ByteArray(size) { 7 }.also {
        (if (path.endsWith(".gguf")) "GGUF" else "lmgg").toByteArray().copyInto(it)
    }
    private fun manager(
        replace: (File, File) -> Boolean = { a, b -> a.renameTo(b) },
        open: (String) -> java.io.InputStream = { ByteArrayInputStream(bytes(it)) }
    ) = ModelManager(temporary.root, { 128 }, { 4 }, replace, open)

    @Test fun interruptedCopyPreservesOldDestinationAndCanRetryAfterRecreation() {
        val old = bytes(".gguf", 64)
        File(temporary.root, "jarvis.gguf").writeBytes(old)
        val broken = manager(open = { path -> object : ByteArrayInputStream(bytes(path)) {
            var reads = 0
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                if (reads++ > 0) throw IOException("synthetic interruption")
                return super.read(buffer, offset, 32)
            }
        } })
        assertThrows(IOException::class.java) { broken.ensureBundledModels() }
        assertArrayEquals(old, broken.llmFile.readBytes())
        assertFalse(broken.hasLlm())
        assertFalse(File(temporary.root, "jarvis.gguf.part").exists())
        val reopened = manager()
        reopened.ensureBundledModels()
        assertTrue(reopened.hasLlm()); assertTrue(reopened.hasWhisper())
        assertArrayEquals(bytes(".gguf"), reopened.llmFile.readBytes())
    }

    @Test fun failedAtomicReplacementNeverDeletesPreviousFile() {
        val old = bytes(".gguf", 64)
        File(temporary.root, "jarvis.gguf").writeBytes(old)
        val broken = manager(replace = { _, _ -> false })
        assertThrows(IllegalStateException::class.java) { broken.ensureBundledModels() }
        assertArrayEquals(old, broken.llmFile.readBytes())
        assertFalse(File(temporary.root, "jarvis.gguf.part").exists())
        manager().ensureBundledModels()
        assertTrue(manager().hasLlm())
    }

    @Test fun abandonedPartialFileIsRestartedAndNeverPromotedBlindly() {
        File(temporary.root, "jarvis.gguf.part").writeBytes(ByteArray(1024))
        manager().ensureBundledModels()
        assertArrayEquals(bytes(".gguf"), manager().llmFile.readBytes())
        assertFalse(File(temporary.root, "jarvis.gguf.part").exists())
    }

    @Test fun truncatedAssetCannotBeInstalledOrReportedReady() {
        val m = manager(open = { ByteArrayInputStream(bytes(it, 64)) })
        assertThrows(IllegalArgumentException::class.java) { m.ensureBundledModels() }
        assertFalse(m.hasLlm()); assertFalse(m.llmFile.exists())
    }

    @Test fun wrongMagicAndOversizedAssetCannotReplaceExistingData() {
        val old = bytes(".gguf", 64)
        File(temporary.root, "jarvis.gguf").writeBytes(old)
        for (data in listOf(ByteArray(128), bytes(".gguf", 256))) {
            val m = manager(open = { ByteArrayInputStream(data) })
            assertThrows(IllegalArgumentException::class.java) { m.ensureBundledModels() }
            assertArrayEquals(old, m.llmFile.readBytes())
            assertFalse(m.hasLlm())
        }
    }

    @Test fun completeFilesArePreservedWithoutOpeningTheirAssetsAgain() {
        manager().ensureBundledModels()
        manager(open = { error("Existing valid model must be kept") }).ensureBundledModels()
        assertTrue(manager().hasWhisper())
    }
}
