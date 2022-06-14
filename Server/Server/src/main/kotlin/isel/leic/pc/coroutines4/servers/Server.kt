package isel.leic.pc.coroutines4.servers

import kotlinx.coroutines.*
import mu.KotlinLogging
import java.lang.Thread.sleep
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousChannelGroup
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.charset.Charset
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


private val logger = KotlinLogging.logger {}
private val charSet = Charset.defaultCharset()
private val decoder = charSet.newDecoder()

class Server(private val port : Int) {

    internal enum class Status {
        NotStarted, Starting, Started, Ending, Ended
    }

    private val exitCmd = "exit"
    private val byeMsg = "bye" + System.lineSeparator()
    private val semaphore = AsyncSemaphore(5)
    @Volatile
    private var status = Status.NotStarted
    @Volatile
    private var nextClientId = 0
    private val logger = KotlinLogging.logger {}
    private val rooms = RoomSet()
    private var currentRoom : Room ? = null

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

    private suspend fun acceptLoop(serverChannel : AsynchronousServerSocketChannel) {
        logger.info("Accept thread started")
        val clients = ConcurrentHashMap.newKeySet<ConnectedClient>()
        status = Status.Started

        while (status == Status.Started) {
            try {
                logger.info("Waiting for client")
                val clientName = "client-${nextClientId++}"
                logger.info("New client accepted $clientName")
                val clientChannel = accept(serverChannel)
                logger.info("client ${clientChannel.remoteAddress} connected")
                val client = ConnectedClient(clientName, clientChannel , rooms)
                scope.launch {
                    logger.info("ConnectedClient readLoop initialization")
                    client.remoteReadLoop()
                }
                scope.launch {
                    logger.info("ConnectedClient mainLoop initialization")
                    client.mainLoop()
                }
                logger.info("client ${clientChannel.remoteAddress} connected")
                clients.add(client)
            } catch (e: Exception) {
                logger.info("Exception caught ${e.message}, which may happen when the listener is closed, continuing...")
            }
        }
        logger.info("Waiting for clients to end, before ending accept loop");
        clients.forEach { client ->
            client.exit();
            client.join();
        }

        logger.info("Accept thread ending");
        status = Status.Ended;
    }

    fun start() {
        suspend fun run() {
            acceptLoop(serverChannel)
        }

        if (status != Status.NotStarted) {
            throw Exception("Server has already started")
        }
        status = Status.Starting
        serverChannel.bind(InetSocketAddress("0.0.0.0", port))

        scope.launch {
            try {
                run()
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
        if (status == Status.NotStarted) {
            throw Exception("Server has not started")
        }
        serverChannel.close()
        sleep(1000)
        group.shutdown()
        group.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS)
    }

    /* TODO :
    fun join() {
        if (status == Status.NotStarted)
        {
            logger.info("Server has not started");
            throw Exception("Server has not started");
        }
        // FIXME what if it is starting?
        if (acceptThread == null)
        {
            logger.LogError("Unexpected state: acceptThread is not set");
            throw new Exception("Unexpected state");
        }
        _acceptThread.Join();
    }*/

}


private fun main() {
    val server = Server(8080)
    server.start()
    readLine()
    logger.info("Server terminated")
}