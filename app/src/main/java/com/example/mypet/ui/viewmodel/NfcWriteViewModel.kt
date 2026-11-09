package com.example.mypet.ui.viewmodel

import android.nfc.Tag
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mypet.data.nfc.NfcTagWriter
import com.example.mypet.data.nfc.NfcWriteOperation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class NfcAvailability { UNSUPPORTED, DISABLED, AVAILABLE }
enum class NfcWritePhase { IDLE, UNSUPPORTED, DISABLED, WAITING, WRITING, CONFIRM, SUCCESS, ERROR }

data class NfcWriteState(
    val petId: String? = null,
    val url: String? = null,
    val phase: NfcWritePhase = NfcWritePhase.IDLE,
    val message: String = "",
    val attempt: Long = 0
) {
    val reading: Boolean get() = phase == NfcWritePhase.WAITING || phase == NfcWritePhase.WRITING
}

class NfcWriteViewModel : ViewModel() {
    var state by mutableStateOf(NfcWriteState())
        private set
    private var availability = NfcAvailability.UNSUPPORTED
    private val writer = NfcTagWriter()
    private var writeJob: Job? = null
    private var timeoutJob: Job? = null
    private var overwriteTagId: ByteArray? = null
    private var pendingOverwriteTagId: ByteArray? = null
    @Volatile private var generation = 0L

    fun updateAvailability(value: NfcAvailability) {
        availability = value
        if (state.url == null || state.phase == NfcWritePhase.SUCCESS) return
        if (value != NfcAvailability.AVAILABLE) {
            stopOperation()
            showAvailability()
        } else if (state.phase == NfcWritePhase.DISABLED || state.phase == NfcWritePhase.UNSUPPORTED) {
            state = state.copy(phase = NfcWritePhase.ERROR, message = "NFC está disponible. Pulsa «Volver a intentar» y acerca el collar.")
        }
    }

    fun start(petId: String, url: String) {
        stopOperation()
        overwriteTagId = null
        pendingOverwriteTagId = null
        state = NfcWriteState(petId = petId, url = url, attempt = generation)
        retry()
    }

    fun retry() {
        if (state.url == null || state.phase == NfcWritePhase.WRITING) return
        stopOperation()
        if (availability != NfcAvailability.AVAILABLE) {
            showAvailability()
            return
        }
        state = state.copy(
            phase = NfcWritePhase.WAITING, attempt = generation,
            message = "Acerca el collar a la parte posterior de tu teléfono y mantenlo quieto."
        )
        scheduleTimeout(30_000, "No se detectó el collar. Acércalo a la zona NFC del teléfono, retira una funda gruesa si la hay y vuelve a intentar.")
    }

    fun onTag(tag: Tag, attempt: Long) {
        if (state.phase != NfcWritePhase.WAITING || attempt != generation) return
        val url = state.url ?: return
        val allowedTagId = overwriteTagId?.copyOf()
        timeoutJob?.cancel()
        state = state.copy(phase = NfcWritePhase.WRITING, message = "Programando y verificando el collar. Mantén el teléfono cerca.")
        scheduleTimeout(15_000, "La operación tardó demasiado. El collar podría haberse escrito; vuelve a intentar para verificarlo.")
        writeJob = viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                writer.write(tag, url, allowedTagId) { generation != attempt }
            }
            if (generation != attempt || state.phase != NfcWritePhase.WRITING) return@launch
            timeoutJob?.cancel()
            state = when (result) {
                NfcWriteOperation.Result.Verified -> state.copy(
                    phase = NfcWritePhase.SUCCESS, message = "El collar está listo. Se verificó el enlace de esta mascota."
                )
                NfcWriteOperation.Result.ConfirmOverwrite -> {
                    pendingOverwriteTagId = tag.id.copyOf()
                    state.copy(
                        phase = NfcWritePhase.CONFIRM,
                        message = "Esta etiqueta ya contiene información. ¿Quieres reemplazarla por el enlace de esta mascota? Después de confirmar, retira y vuelve a acercar el mismo collar."
                    )
                }
                NfcWriteOperation.Result.Cancelled -> state.copy(
                    phase = NfcWritePhase.ERROR, message = "La operación se interrumpió. Vuelve a intentar para verificar el collar."
                )
                is NfcWriteOperation.Result.Failed -> state.copy(phase = NfcWritePhase.ERROR, message = result.message)
            }
        }
    }

    fun confirmOverwrite() {
        if (state.phase != NfcWritePhase.CONFIRM) return
        overwriteTagId = pendingOverwriteTagId?.copyOf()
        pendingOverwriteTagId = null
        retry()
    }

    fun pause() {
        if (!state.reading) return
        stopOperation()
        state = state.copy(
            phase = NfcWritePhase.ERROR,
            message = "La programación se interrumpió al salir de la pantalla. Vuelve a intentar para comprobar el collar."
        )
    }

    fun readerFailed() {
        stopOperation()
        state = state.copy(phase = NfcWritePhase.ERROR, message = "No se pudo activar el lector NFC. Comprueba que NFC esté encendido y vuelve a intentar.")
    }

    fun dismiss(petId: String? = state.petId) {
        if (petId != state.petId) return
        stopOperation()
        overwriteTagId = null
        pendingOverwriteTagId = null
        state = NfcWriteState(attempt = generation)
    }

    private fun showAvailability() {
        state = if (availability == NfcAvailability.UNSUPPORTED) {
            state.copy(phase = NfcWritePhase.UNSUPPORTED, message = "Este teléfono no tiene NFC. Para programar el collar, abre MyPet en un teléfono con NFC.")
        } else {
            state.copy(phase = NfcWritePhase.DISABLED, message = "Activa NFC en los ajustes del teléfono y vuelve a MyPet.")
        }
    }

    private fun scheduleTimeout(milliseconds: Long, message: String) {
        timeoutJob?.cancel()
        val attempt = generation
        timeoutJob = viewModelScope.launch {
            delay(milliseconds)
            if (generation == attempt && state.reading) {
                stopOperation()
                state = state.copy(phase = NfcWritePhase.ERROR, message = message)
            }
        }
    }

    private fun stopOperation() {
        generation++
        timeoutJob?.cancel()
        writeJob?.cancel()
        writer.cancel()
    }

    override fun onCleared() {
        stopOperation()
        super.onCleared()
    }
}
