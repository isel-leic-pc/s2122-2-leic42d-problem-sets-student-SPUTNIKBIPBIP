package isel.leic.pc.coroutines4.servers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.charset.Charset

private val logger = KotlinLogging.logger {}

fun closeConnection(connectionChannel : AsynchronousSocketChannel) {
    logger.info("client ${connectionChannel.remoteAddress} disconnected")
    connectionChannel.close()
}

suspend fun close(channel: AsynchronousSocketChannel) {
    withContext(Dispatchers.IO) {
        channel.close()
    }
}

fun putBuffer(buffer: ByteBuffer, text: String) {
    buffer.clear()
    buffer.put(text.toByteArray(Charset.defaultCharset()))
    buffer.flip()
}