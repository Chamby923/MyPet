package com.example.mypet

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mypet.data.nfc.NfcTagWriter
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NfcUriMessageTest {
    private val url = "https://mypet-pip.web.app/?p=" + "a".repeat(32)

    @Test
    fun encodesExactlyOneUriAndFitsNtag213() {
        val message = NfcTagWriter.createMessage(url)
        assertEquals(1, message.records.size)
        assertEquals(NdefRecord.TNF_WELL_KNOWN, message.records[0].tnf)
        assertArrayEquals(NdefRecord.RTD_URI, message.records[0].type)
        assertEquals(url, message.records[0].toUri().toString())
        assertEquals(58, message.toByteArray().size)
    }

    @Test
    fun roundTripPreservesThePublishedIdentifier() {
        val restored = NdefMessage(NfcTagWriter.createMessage(url).toByteArray())
        assertEquals(url, restored.records.single().toUri().toString())
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonMypetUri() {
        NfcTagWriter.createMessage("https://example.invalid/?p=" + "a".repeat(32))
    }
}
