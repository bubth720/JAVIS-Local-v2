package com.stark.jarvislocal

import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ModelManagerConcurrencyTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun twoCallersCannotWriteTheSamePartFileConcurrently() {
        val firstOpened = CountDownLatch(1)
        val continueCopy = CountDownLatch(1)
        val secondStarted = CountDownLatch(1)
        val opens = AtomicInteger()
        val active = AtomicInteger()
        val maximum = AtomicInteger()
        val bytes = ByteArray(128) { it.toByte() }
        val manager = ModelManager(temporary.root, { 128 }, { 4 }) { path ->
            val count = active.incrementAndGet()
            maximum.updateAndGet { maxOf(it, count) }
            if (opens.incrementAndGet() == 1) {
                firstOpened.countDown()
                check(continueCopy.await(5, TimeUnit.SECONDS))
            }
            object : ByteArrayInputStream(modelBytes(path, bytes)) {
                override fun close() { active.decrementAndGet(); super.close() }
            }
        }
        val executor = Executors.newFixedThreadPool(2)
        try {
            val first = executor.submit { manager.ensureBundledModels() }
            assertTrue(firstOpened.await(2, TimeUnit.SECONDS))
            val second = executor.submit { secondStarted.countDown(); manager.ensureBundledModels() }
            assertTrue(secondStarted.await(2, TimeUnit.SECONDS))
            assertFalse(second.isDone)
            assertEquals(1, opens.get())
            continueCopy.countDown()
            first.get(5, TimeUnit.SECONDS); second.get(5, TimeUnit.SECONDS)
            assertEquals(1, maximum.get())
            assertArrayEquals(modelBytes(".gguf", bytes), manager.llmFile.readBytes())
            assertArrayEquals(modelBytes(".bin", bytes), manager.whisperFile.readBytes())
            assertFalse(temporary.root.listFiles().orEmpty().any { it.name.endsWith(".part") })
        } finally { continueCopy.countDown(); executor.shutdownNow() }
    }

    @Test fun copyErrorReleasesLockForRetry() {
        var fail = true
        val bytes = ByteArray(128)
        val manager = ModelManager(temporary.root, { 128 }, { 4 }) { path ->
            if (fail) throw IOException("asset unavailable")
            ByteArrayInputStream(modelBytes(path, bytes))
        }
        try { manager.ensureBundledModels(); fail("Copy should fail") } catch (e: IOException) { }
        fail = false
        manager.ensureBundledModels()
        assertArrayEquals(modelBytes(".gguf", bytes), manager.llmFile.readBytes())
        assertArrayEquals(modelBytes(".bin", bytes), manager.whisperFile.readBytes())
    }

    private fun modelBytes(path: String, bytes: ByteArray): ByteArray = bytes.copyOf().also {
        (if (path.endsWith(".gguf")) "GGUF" else "lmgg").toByteArray().copyInto(it)
    }
}
