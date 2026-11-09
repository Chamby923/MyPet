package com.example.mypet.data.nfc

import java.io.Closeable
import java.io.IOException
import java.util.concurrent.CancellationException
import java.util.concurrent.FutureTask
import java.util.concurrent.atomic.AtomicBoolean

/** Flujo de escritura comprobable sin hardware. El adaptador Android maneja NDEF. */
class NfcWriteOperation {
    class InvalidFormatException : IOException("Formato NDEF no válido")
    interface Connection : Closeable {
        val writable: Boolean
        val capacity: Int
        fun connect()
        fun read(): ByteArray?
        fun write(message: ByteArray)
    }

    sealed interface Result {
        data object Verified : Result
        data object ConfirmOverwrite : Result
        data object Cancelled : Result
        data class Failed(val message: String) : Result
    }

    private val busy = AtomicBoolean(false)
    private val lock = Any()
    private var activeClose: FutureTask<Unit>? = null

    fun write(
        connection: Connection,
        message: ByteArray,
        allowOverwrite: Boolean,
        cancelled: () -> Boolean
    ): Result {
        if (!busy.compareAndSet(false, true)) {
            return Result.Failed("La operación anterior está terminando. Retira el collar y vuelve a intentar.")
        }
        val closing = FutureTask<Unit> { connection.close() }
        try {
            synchronized(lock) {
                if (cancelled()) return Result.Cancelled
                activeClose = closing
            }
            checkActive(cancelled)
            connection.connect()
            checkActive(cancelled)
            if (!connection.writable) {
                return Result.Failed("Esta etiqueta es de solo lectura. Usa un collar que permita escritura.")
            }
            if (message.size > connection.capacity) {
                return Result.Failed("Esta etiqueta no tiene espacio suficiente para el enlace de MyPet.")
            }
            val existing = connection.read()
            checkActive(cancelled)
            if (existing?.contentEquals(message) == true) return Result.Verified
            if (existing != null && !allowOverwrite) return Result.ConfirmOverwrite

            checkActive(cancelled)
            connection.write(message)
            checkActive(cancelled)
            val actual = connection.read()
            checkActive(cancelled)
            return if (actual?.contentEquals(message) == true) {
                Result.Verified
            } else {
                Result.Failed("No se pudo verificar el enlace. Mantén el collar cerca y vuelve a intentar.")
            }
        } catch (_: CancellationException) {
            return Result.Cancelled
        } catch (_: InvalidFormatException) {
            return Result.Failed("La etiqueta contiene un formato NDEF no válido. Usa otra etiqueta preparada para guardar enlaces.")
        } catch (_: IOException) {
            if (cancelled()) return Result.Cancelled
            return Result.Failed("Se interrumpió la comunicación con el collar. Retíralo y vuelve a acercarlo sin moverlo.")
        } catch (_: SecurityException) {
            return Result.Failed("Android no permitió acceder a NFC. Comprueba que siga activado.")
        } catch (_: IllegalStateException) {
            return Result.Failed("La etiqueta dejó de estar disponible. Retira el collar y vuelve a intentar.")
        } finally {
            synchronized(lock) {
                if (activeClose === closing) activeClose = null
            }
            // cancel() y finally comparten un único cierre. El reintento espera a que termine.
            closing.run()
            runCatching { closing.get() }
            busy.set(false)
        }
    }

    fun cancel() {
        val closing = synchronized(lock) {
            activeClose.also { activeClose = null }
        } ?: return
        // close() interrumpe I/O pendiente y puede bloquear. Se ejecuta fuera de la interfaz.
        Thread(closing, "MyPet-NfcClose").start()
    }

    private fun checkActive(cancelled: () -> Boolean) {
        if (cancelled()) throw CancellationException()
    }
}
