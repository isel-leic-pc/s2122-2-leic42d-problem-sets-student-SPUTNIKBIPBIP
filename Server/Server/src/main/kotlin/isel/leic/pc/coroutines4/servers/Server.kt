package isel.leic.pc.coroutines4.servers

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousChannelGroup
import java.nio.channels.AsynchronousServerSocketChannel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger


private val logger = KotlinLogging.logger("Server")

class Server(private val port : Int) {

    internal enum class Status {
        NotStarted, Starting, Started, Ending, Ended
    }

    private var status = Status.NotStarted
    private var nextClientId = AtomicInteger(0)
    private val logger = KotlinLogging.logger {}
    private val rooms = RoomSet()
    private val lock = Mutex()
    private lateinit var loopJob: Job
    val clients = ConcurrentHashMap<Int, ConnectedClient>()


    companion object Notifier {

        private lateinit var server : Server
        private val lock = Mutex()

        suspend fun nofityServerTermination(timeout: Long) {
            lock.withLock {
                if (server.status != Status.Started) {
                    throw Exception("Server has not been started yet")
                }
                server.status = Status.Ending
            }
            server.stopServer(timeout)
        }
    }

    init {
        server = this
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


    private suspend fun acceptLoop(serverChannel : AsynchronousServerSocketChannel) {
        logger.info("Accept thread started")
        lock.withLock {
            if (status != Status.Starting) {
                throw Exception("Server isn't starting")
            }
            status = Status.Started
        }
        while (status == Status.Started) {
            try {
                logger.info("Waiting for client")
                val clientName = "client-${nextClientId.getAndIncrement()}"
                val clientChannel = accept(serverChannel)
                logger.info("New client accepted $clientName")
                val client = ConnectedClient(clientName, clientChannel , rooms)
                client.start()
                logger.info("client ${clientChannel.remoteAddress} connected")
                clients.putIfAbsent(nextClientId.get(), client)
            } catch (e: Exception) {
                logger.info("Exception caught ${e.message}, which may happen when the listener is closed, continuing...")
            }
        }
        lock.withLock {
            if (status != Status.Started) {
                throw Exception("Server isn't running")
            }
            status = Status.Ending;
        }
        stopServer(2)
    }

    suspend fun start() {
        suspend fun run() {
            acceptLoop(serverChannel)
        }
        lock.withLock {
            if (status != Status.NotStarted) {
                throw Exception("Server has already started")
            }
            status = Status.Starting
        }

        serverChannel.bind(InetSocketAddress("0.0.0.0", port))

        loopJob = scope.launch {
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

    private suspend fun stopServer(timeout : Long = 5) {
        logger.info("Waiting for clients to end, before ending accept loop");
        clients.forEach { (_, client) ->
            client.exit()
        }
        logger.info("Accept thread ending");
        lock.withLock {
            if (status != Status.Ending) {
                throw Exception("Server can't stop, status = $status")
            }
            status = Status.Ended
        }
        loopJob.cancelAndJoin()
        serverChannel.close()
        group.shutdown()
        group.awaitTermination(timeout, TimeUnit.MILLISECONDS)
    }
}


private suspend fun main() {
    val server = Server(8080)
    server.start()
    readLine()
    logger.info("Server terminated")
}