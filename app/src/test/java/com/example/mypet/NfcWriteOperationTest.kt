package com.example.mypet

import com.example.mypet.data.nfc.NfcWriteOperation
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class NfcWriteOperationTest {
    private val message = byteArrayOf(1, 2, 3)

    private open class FakeConnection(
        override val writable: Boolean = true,
        override val capacity: Int = 144,
        var content: ByteArray? = null
    ) : NfcWriteOperation.Connection {
        var connects = 0
        var reads = 0
        var writes = 0
        val closes = AtomicInteger()
        override fun connect() { connects++ }
        override fun read(): ByteArray? { reads++; return content }
        override fun write(message: ByteArray) { writes++; content = message.copyOf() }
        override fun close() { closes.incrementAndGet() }
    }

    @Test
    fun writesAndReadsBackBeforeReportingSuccess() {
        val connection = FakeConnection()
        val result = NfcWriteOperation().write(connection, message, false) { false }
        assertEquals(NfcWriteOperation.Result.Verified, result)
        assertEquals(1, connection.writes)
        assertEquals(2, connection.reads)
        assertEquals(1, connection.closes.get())
    }

    @Test
    fun readBackMismatchIsNeverSuccess() {
        val connection = object : FakeConnection() {
            override fun read(): ByteArray? {
                return if (writes == 0) null else byteArrayOf(9)
            }
        }
        assertTrue(NfcWriteOperation().write(connection, message, false) { false } is NfcWriteOperation.Result.Failed)
        assertEquals(1, connection.writes)
        assertEquals(1, connection.closes.get())
    }

    @Test
    fun refusesReadOnlyOrInsufficientCapacity() {
        for (connection in listOf(FakeConnection(writable = false), FakeConnection(capacity = 2))) {
            assertTrue(NfcWriteOperation().write(connection, message, false) { false } is NfcWriteOperation.Result.Failed)
            assertEquals(0, connection.writes)
            assertEquals(1, connection.closes.get())
        }
    }

    @Test
    fun differentContentNeedsExplicitOverwritePermission() {
        val operation = NfcWriteOperation()
        val connection = FakeConnection(content = byteArrayOf(9))
        assertEquals(NfcWriteOperation.Result.ConfirmOverwrite, operation.write(connection, message, false) { false })
        assertEquals(0, connection.writes)
        assertEquals(NfcWriteOperation.Result.Verified, operation.write(connection, message, true) { false })
        assertEquals(1, connection.writes)
    }

    @Test
    fun correctExistingMessageIsVerifiedWithoutWritingAgain() {
        val connection = FakeConnection(content = message.copyOf())
        assertEquals(NfcWriteOperation.Result.Verified, NfcWriteOperation().write(connection, message, false) { false })
        assertEquals(0, connection.writes)
        assertEquals(1, connection.reads)
    }

    @Test
    fun cancellationBeforeWritingPreventsWrite() {
        var cancelled = false
        val connection = object : FakeConnection() {
            override fun read(): ByteArray? { cancelled = true; return null }
        }
        assertEquals(NfcWriteOperation.Result.Cancelled, NfcWriteOperation().write(connection, message, false) { cancelled })
        assertEquals(0, connection.writes)
        assertEquals(1, connection.closes.get())
    }

    @Test
    fun failureOrMalformedMessageNeverReportsSuccess() {
        for (error in listOf(IOException(), NfcWriteOperation.InvalidFormatException())) {
            val connection = object : FakeConnection() {
                override fun write(message: ByteArray) { throw error }
            }
            assertTrue(NfcWriteOperation().write(connection, message, false) { false } is NfcWriteOperation.Result.Failed)
            assertEquals(1, connection.closes.get())
        }
    }

    @Test
    fun simultaneousDetectionCannotStartAnotherWrite() {
        val operation = NfcWriteOperation()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val first = object : FakeConnection() {
            override fun read(): ByteArray? {
                entered.countDown()
                check(release.await(3, TimeUnit.SECONDS))
                return super.read()
            }
        }
        val task = FutureTask { operation.write(first, message, false) { false } }
        Thread(task).start()
        try {
            assertTrue(entered.await(3, TimeUnit.SECONDS))
            val duplicate = FakeConnection()
            assertTrue(operation.write(duplicate, message, false) { false } is NfcWriteOperation.Result.Failed)
            assertEquals(0, duplicate.connects)
        } finally {
            release.countDown()
        }
        assertEquals(NfcWriteOperation.Result.Verified, task.get(3, TimeUnit.SECONDS))
        assertEquals(1, first.writes)
    }

    @Test
    fun cancellationClosesBlockedConnectionOffCallerThread() {
        val operation = NfcWriteOperation()
        val entered = CountDownLatch(1)
        val closed = CountDownLatch(1)
        val cancelled = AtomicBoolean()
        val connection = object : FakeConnection() {
            override fun read(): ByteArray? {
                entered.countDown()
                check(closed.await(3, TimeUnit.SECONDS))
                throw IOException()
            }
            override fun close() { super.close(); closed.countDown() }
        }
        val task = FutureTask { operation.write(connection, message, false) { cancelled.get() } }
        Thread(task).start()
        assertTrue(entered.await(3, TimeUnit.SECONDS))
        cancelled.set(true)
        operation.cancel()
        assertEquals(NfcWriteOperation.Result.Cancelled, task.get(3, TimeUnit.SECONDS))
        assertEquals(1, connection.closes.get())
        assertEquals(0, connection.writes)
    }

    @Test
    fun retryWaitsForCancelledConnectionToFinishClosing() {
        val operation = NfcWriteOperation()
        val entered = CountDownLatch(1)
        val closeStarted = CountDownLatch(1)
        val releaseClose = CountDownLatch(1)
        val cancelled = AtomicBoolean()
        val connection = object : FakeConnection() {
            override fun read(): ByteArray? {
                entered.countDown()
                check(closeStarted.await(3, TimeUnit.SECONDS))
                throw IOException()
            }
            override fun close() {
                super.close()
                closeStarted.countDown()
                check(releaseClose.await(3, TimeUnit.SECONDS))
            }
        }
        val task = FutureTask { operation.write(connection, message, false) { cancelled.get() } }
        Thread(task).start()
        try {
            assertTrue(entered.await(3, TimeUnit.SECONDS))
            cancelled.set(true)
            operation.cancel()
            assertTrue(closeStarted.await(3, TimeUnit.SECONDS))
            val retry = FakeConnection()
            assertTrue(operation.write(retry, message, false) { false } is NfcWriteOperation.Result.Failed)
            assertEquals(0, retry.connects)
        } finally {
            releaseClose.countDown()
        }
        assertEquals(NfcWriteOperation.Result.Cancelled, task.get(3, TimeUnit.SECONDS))
        assertEquals(1, connection.closes.get())
        assertEquals(NfcWriteOperation.Result.Verified, operation.write(FakeConnection(), message, false) { false })
    }
}
