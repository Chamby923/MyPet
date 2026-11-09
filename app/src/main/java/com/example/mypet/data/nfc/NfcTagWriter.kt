package com.example.mypet.data.nfc

import android.nfc.FormatException
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef

class NfcTagWriter {
    private val operation = NfcWriteOperation()

    fun write(
        tag: Tag,
        url: String,
        overwriteTagId: ByteArray?,
        cancelled: () -> Boolean
    ): NfcWriteOperation.Result {
        val message = try {
            createMessage(url).toByteArray()
        } catch (_: IllegalArgumentException) {
            return NfcWriteOperation.Result.Failed("La ficha no tiene un enlace público válido de MyPet.")
        }
        val ndef = Ndef.get(tag) ?: return NfcWriteOperation.Result.Failed(
            "Esta etiqueta no tiene un formato NDEF compatible. Usa una etiqueta NTAG213 preparada para guardar enlaces."
        )
        val result = operation.write(
            AndroidConnection(ndef), message,
            overwriteTagId != null && overwriteTagId.isNotEmpty() && overwriteTagId.contentEquals(tag.id),
            cancelled
        )
        return if (result == NfcWriteOperation.Result.ConfirmOverwrite && tag.id.isEmpty()) {
            NfcWriteOperation.Result.Failed("No se pudo identificar esta etiqueta para confirmar el reemplazo. Usa otra etiqueta.")
        } else {
            result
        }
    }

    fun cancel() = operation.cancel()

    companion object {
        fun createMessage(url: String): NdefMessage {
            require(url.matches(Regex("https://mypet-pip\\.web\\.app/\\?p=[A-Za-z0-9_-]{22,64}")))
            return NdefMessage(arrayOf(NdefRecord.createUri(url)))
        }
    }

    private class AndroidConnection(private val ndef: Ndef) : NfcWriteOperation.Connection {
        override val writable: Boolean get() = ndef.isWritable
        override val capacity: Int get() = ndef.maxSize
        override fun connect() = ndef.connect()
        override fun close() = ndef.close()

        override fun read(): ByteArray? = ndefIo {
            // Lectura real por radio. cachedNdefMessage contiene datos anteriores a la escritura.
            val message = ndef.ndefMessage
            if (message == null || message.records.all { it.tnf == NdefRecord.TNF_EMPTY }) {
                null
            } else {
                message.toByteArray()
            }
        }

        override fun write(message: ByteArray) = ndefIo {
            ndef.writeNdefMessage(NdefMessage(message))
        }

        private fun <T> ndefIo(block: () -> T): T = try {
            block()
        } catch (_: FormatException) {
            throw NfcWriteOperation.InvalidFormatException()
        }
    }
}
