package isel.leic.pc.coroutines4.servers

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.nio.charset.Charset
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.random.Random

private val random = Random.Default
private val logger = KotlinLogging.logger {}
private val charSet = Charset.defaultCharset()
private val decoder = charSet.newDecoder()
private val encoder = charSet.newEncoder()


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
        logger.info("breakpoint")
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

suspend fun read(clientChannel : AsynchronousSocketChannel) : String {
    val buffer = ByteBuffer.allocate(1024)
    read(clientChannel, buffer)
    val res = withContext(Dispatchers.IO) {
        decoder.decode(buffer.flip())
    }.toString().trim()
    logger.info("stop here")
    return res
}

suspend fun write(clientChannel : AsynchronousSocketChannel, text : String) {
    val buffer = CharBuffer.allocate(1024)
    val byteBuffer = withContext(Dispatchers.IO) {
        encoder.encode(buffer.put(text + "\r\n").flip())
    }
    write(clientChannel, byteBuffer)
}

