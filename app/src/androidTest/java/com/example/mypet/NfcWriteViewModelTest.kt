package com.example.mypet

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mypet.ui.viewmodel.NfcAvailability
import com.example.mypet.ui.viewmodel.NfcWritePhase
import com.example.mypet.ui.viewmodel.NfcWriteViewModel
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NfcWriteViewModelTest {
    private val viewModel = NfcWriteViewModel()
    private val url = "https://mypet-pip.web.app/?p=" + "a".repeat(32)

    private fun open(availability: NfcAvailability) {
        onMain {
            viewModel.updateAvailability(availability)
            viewModel.start("test-pet", url)
        }
    }

    @After
    fun cancelPendingSession() {
        onMain { viewModel.dismiss() }
    }

    @Test
    fun explainsMissingHardwareAndAllowsClosing() {
        open(NfcAvailability.UNSUPPORTED)
        onMain {
            assertEquals(NfcWritePhase.UNSUPPORTED, viewModel.state.phase)
            assertTrue(viewModel.state.message.contains("no tiene NFC"))
            assertFalse(viewModel.state.reading)
            viewModel.dismiss()
            assertNull(viewModel.state.url)
        }
    }

    @Test
    fun waitsPausesRetriesAndCancelsWithoutKeepingSessionArmed() {
        open(NfcAvailability.AVAILABLE)
        onMain {
            assertTrue(viewModel.state.message.contains("Acerca el collar"))
            val attempt = viewModel.state.attempt
            assertTrue(viewModel.state.reading)
            viewModel.pause()
            assertFalse(viewModel.state.reading)
            viewModel.retry()
            assertEquals(NfcWritePhase.WAITING, viewModel.state.phase)
            assertTrue(viewModel.state.attempt > attempt)
            viewModel.dismiss()
            assertEquals(NfcWritePhase.IDLE, viewModel.state.phase)
            assertFalse(viewModel.state.reading)
        }
    }

    @Test
    fun offersSettingsAndRequiresRetryAfterEnablingNfc() {
        open(NfcAvailability.DISABLED)
        onMain {
            assertEquals(NfcWritePhase.DISABLED, viewModel.state.phase)
            assertTrue(viewModel.state.message.contains("Activa NFC"))
            assertFalse(viewModel.state.reading)
            viewModel.updateAvailability(NfcAvailability.AVAILABLE)
            assertEquals(NfcWritePhase.ERROR, viewModel.state.phase)
            viewModel.retry()
            assertEquals(NfcWritePhase.WAITING, viewModel.state.phase)
        }
    }

    private fun onMain(block: () -> Unit) {
        var failure: Throwable? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            try {
                block()
            } catch (error: Throwable) {
                failure = error
            }
        }
        failure?.let { throw it }
    }
}
