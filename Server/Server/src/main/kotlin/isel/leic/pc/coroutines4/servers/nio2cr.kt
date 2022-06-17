package isel.leic.pc.coroutines4.servers

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.nio.channels.InterruptedByTimeoutException
import java.nio.charset.Charset
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.random.Random

private val random = Random.Default
private val logger = LoggerFactory.getLogger("NIO")
private val charSet = Charset.defaultCharset()
private val encoder = Charsets.UTF_8.newEncoder()
private val decoder = Charsets.UTF_8.newDecoder()


/**
 * The completion handler implementation for reads
 */
private val readHandler = object : CompletionHandler<Int, CancellableContinuation<Int>> {
    override fun completed(result: Int, attachment: CancellableContinuation<Int>) {
        logger.info("on read callback")
        attachment.resume(result)
    }

    override fun failed(exc: Throwable, attachment: CancellableContinuation<Int>) {
        attachment.resumeWithException(exc)
    }
}

private val writeHandler = object : CompletionHandler<Int, CancellableContinuation<Int>> {
    override fun completed(result: Int, attachment: CancellableContinuation<Int>) {
        logger.info("on write callback")
        attachment.resume(result)
    }

    override fun failed(exc: Throwable, attachment: CancellableContinuation<Int>) {
        attachment.resumeWithException(exc)
    }
}

private val acceptHandler = object :
            CompletionHandler<AsynchronousSocketChannel,
            CancellableContinuation<AsynchronousSocketChannel>> {
    override fun completed(result: AsynchronousSocketChannel,
                           attachment: CancellableContinuation<AsynchronousSocketChannel>) {
        logger.info("on accept callback")
        attachment.resume(result)
    }

    override fun failed(exc: Throwable,
                        attachment: CancellableContinuation<AsynchronousSocketChannel>) {
        attachment.resumeWithException(exc)
    }
}

// extension methods for using nio2 with coroutines

suspend fun read(channel: AsynchronousSocketChannel, buffer: ByteBuffer): Int {
    return suspendCancellableCoroutine { cont->
        // close channel on cancellation
        cont.invokeOnCancellation {
            channel.close()
        }
        channel.read(buffer, cont, readHandler)
    }
}

suspend fun write(channel : AsynchronousSocketChannel, buffer: ByteBuffer): Int {
    return suspendCancellableCoroutine { cont->
        // close channel on cancellation
        cont.invokeOnCancellation {
            channel.close()
        }
        channel.write(buffer, cont, writeHandler)
    }
}

suspend fun accept(listener : AsynchronousServerSocketChannel):
        AsynchronousSocketChannel {
    return suspendCancellableCoroutine { cont->
        // close channel on cancellation
        cont.invokeOnCancellation {
            listener.close()
        }
        listener.accept(cont, acceptHandler)
    }
}

suspend fun AsynchronousSocketChannel.write(text: String): Int {
    return suspendCancellableCoroutine { continuation ->
        val toSend = CharBuffer.wrap(text + "\r\n")
        write(encoder.encode(toSend), continuation, writeHandler)
    }
}

suspend fun AsynchronousSocketChannel.read(timeout: Long = 0, unit: TimeUnit = TimeUnit.MILLISECONDS): String? {
    return suspendCancellableCoroutine { continuation ->
        val buffer = ByteBuffer.allocate(1024)
        read(buffer, timeout, unit, null, object : CompletionHandler<Int, Any?> {
            override fun completed(result: Int, attachment: Any?) {
                if (continuation.isCancelled)
                    continuation.resumeWithException(CancellationException())
                else {
                    val received = decoder.decode(buffer.flip()).toString().trim()
                    continuation.resume(received)
                }
            }

            override fun failed(error: Throwable, attachment: Any?) {
                if (error is InterruptedByTimeoutException) {
                    continuation.resume(null)
                }
                else {
                    continuation.resumeWithException(error)
                }
            }
        })
    }
}
