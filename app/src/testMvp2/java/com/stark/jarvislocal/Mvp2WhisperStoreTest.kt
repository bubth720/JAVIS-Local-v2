package com.stark.jarvislocal

import android.app.Application
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[24,25,35],application=Application::class)
class Mvp2WhisperStoreTest {
    @get:Rule val dir=TemporaryFolder()
    private val root get()=File(dir.root,"voice")
    private fun store()=MvpModelStore(root,MvpImportPolicy(24,4096,0),MvpModelFormat.WHISPER_GGML)
    private fun bytes()=ByteArray(128).also{ByteBuffer.wrap(it).order(ByteOrder.LITTLE_ENDIAN)
        .putInt(0x67676d6c).putInt(51865).putInt(1500).putInt(384).putInt(6).putInt(4)}
    private fun manifest(b:ByteArray)=MvpModelManifest("public synthetic fixture","CC0 test only",MessageDigest.getInstance("SHA-256").digest(b)
        .joinToString(""){"%02x".format(it.toInt() and 255)},b.size.toLong())
    @Test fun privateVoiceImportDoesNotImplyWhisperNativeQualification() {
        val b=bytes();store().install(manifest(b),{b.inputStream()},{})
        assertTrue(store().selectedFile().name.endsWith(".bin"));store().verifySelected()
        assertArrayEquals(b,store().selectedFile().readBytes())
    }
    @Test fun wrongHeaderAndHashAreRefusedWithoutSelectingModel() {
        for(b in listOf(ByteArray(128),bytes().also{it[0]=0})){
            try{store().install(manifest(b),{b.inputStream()},{});fail()}catch(_:IllegalArgumentException){}
        }
        val b=bytes();try{store().install(manifest(b).copy(sha256="0".repeat(64)),{b.inputStream()},{});fail()}catch(_:IllegalArgumentException){}
        assertNull(store().selected())
    }
    @Test fun ggufStoreCannotSelectWhisperMetadataAndNoThresholdLowered() {
        val b=bytes();store().install(manifest(b),{b.inputStream()},{})
        try { MvpModelStore(root,MvpImportPolicy(24,4096,0)).selected();fail() }catch(_:IllegalArgumentException){}
        try{manifest(b).validate(MvpImportPolicy(50L*1024*1024,160L*1024*1024));fail()}catch(_:IllegalArgumentException){}
    }
    @Test fun cancellationRetainsPreviousVoiceFileAndManifest() {
        val b=bytes();store().install(manifest(b),{b.inputStream()},{})
        var checks=0
        try{store().install(manifest(b),{b.inputStream()},{if(++checks==3)throw java.util.concurrent.CancellationException()});fail()}
        catch(_:java.util.concurrent.CancellationException){}
        assertEquals(manifest(b),store().selected());assertArrayEquals(b,store().selectedFile().readBytes())
    }
}
