package isel.leic.pc.coroutines4.servers

import kotlinx.coroutines.*
import mu.KotlinLogging
import java.lang.Thread.sleep
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousChannelGroup
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.charset.Charset
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


private val logger = KotlinLogging.logger {}
private val charSet = Charset.defaultCharset()
private val decoder = charSet.newDecoder()
private var currentRoom : Room ? = null


class Server(private val port : Int) {
    private val exitCmd = "exit"
    private val byeMsg = "bye" + System.lineSeparator()
    private val semaphore = AsyncSemaphore(5)

    internal enum class Status {
        NotStarted, Starting, Started, Ending, Ended
    }


    private class ServerScope {

        private val handler = CoroutineExceptionHandler { ctx, exc ->
            logger.info("caught exception $exc: ${exc.message}")
        }

        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + handler)
    }

    private val group =
        AsynchronousChannelGroup.withThreadPool(Executors.newSingleThreadExecutor())
    private val serverChannel = AsynchronousServerSocketChannel.open(group)

    // The parent scope to handler coroutines
    private val scope = ServerScope().scope

    suspend fun handler(clientChannel: AsynchronousSocketChannel) {

        suspend fun getMsg() : String {
            return read(clientChannel)
        }

        suspend fun  bye() {
            write(clientChannel, "bye")
        }

        suspend fun process()  {
            //semaphore.acquire(1000.toDuration(DurationUnit.MILLISECONDS))
            logger.info("Start client processing")
            while(read(clientChannel) > 0) {
                buffer.flip()
                logger.info("msg read from ${clientChannel.remoteAddress}")
                val msg = getMsg()
                when (msg) {

                    "/exit" -> {
                        bye()
                        break
                    }
                }
                write(clientChannel, buffer)
                logger.info("After echo message")
            }
        }
        try {
            process()
        }
        catch(e: Exception) {
            println("error on client handler: $e")
        }
        finally {
            closeConnection(clientChannel)
        }
    }

    fun run() {
        suspend fun runInternal() {
            while(true) {
                val channel = accept(serverChannel)
                logger.info("client ${channel.remoteAddress} connected")

                // connection handler coroutine
                scope.launch {
                    handler(channel)
                }

                // to debug purposes
                //scope.coroutineContext.job.childrenStates()
            }
        }

        serverChannel.bind(InetSocketAddress("0.0.0.0", port))

        // accept connections coroutine
        scope.launch {
            try {
                runInternal()
            }
            catch(e: Exception) {
                println("error on accept: terminate server!")
            }
            finally {
                serverChannel.close()
            }
        }
    }

    fun stop() {
        serverChannel.close()
        sleep(1000)
        group.shutdown()
        group.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS)
    }

}


private fun main() {
    val server = Server(8080)
    server.run()

    readln()
    server.stop()
    logger.info("Server terminated")
}