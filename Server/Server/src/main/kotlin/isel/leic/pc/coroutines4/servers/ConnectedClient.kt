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
import javax.sound.sampled.Line


private val logger = KotlinLogging.logger {}
private val charSet = Charset.defaultCharset()
private val decoder = charSet.newDecoder()
private var currentRoom : Room ? = null


class ConnectedClient(private val port : Int) {
    private val exitCmd = "exit"
    private val byeMsg = "bye" + System.lineSeparator()
    private val semaphore = AsyncSemaphore(5)


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

        // send the bye message to client
        suspend fun  bye() {
           // write(clientChannel, buffer)
        }

        suspend fun process()  {
        //semaphore.acquire(1000.toDuration(DurationUnit.MILLISECONDS))
            logger.info("Start client processing")
            while(read(clientChannel, buffer) > 0) {
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

    //TODO: get access to clientChannel refactor server start
    suspend fun postRoomMessage(formattedMessage: String, room: Room) {
        if (currentRoom != null) {
            write(clientChannel, "Need to be inside a room to post a message")
        } else {
            currentRoom!!.post(this, formattedMessage)
        }
    }



}

private abstract class ControlMessage {

    class RoomMessage(val Sender: Room, val Value: String) : ControlMessage()
    class RemoteLine(val Value : String) : ControlMessage()
    class RemoteInputEnded : ControlMessage()
    {
    }
    class Stop : ControlMessage()
    {
    }
}

private fun main() {
    val server = ConnectedClient(8080)
    server.run()

    readln()
    server.stop()
    logger.info("Server terminated")
}