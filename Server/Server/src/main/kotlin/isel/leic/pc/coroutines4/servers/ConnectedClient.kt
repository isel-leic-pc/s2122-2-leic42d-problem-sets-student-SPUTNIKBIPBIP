package isel.leic.pc.coroutines4.servers


import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.BlockingQueue as ConcurrentBlockingQueue


private abstract class ControlMessage {

    class RoomMessage(val Value: String, val Sender: Room) : ControlMessage()
    class RemoteLine(val Value: String) : ControlMessage()
    class RemoteInputEnded : ControlMessage()
    class Stop : ControlMessage()
}

class ConnectedClient(
    val name: String,
    private val clientChannel: AsynchronousSocketChannel,
    private val rooms: RoomSet
) {



    private var exiting : Boolean = false
    private val logger = KotlinLogging.logger("ConnectedClient")
    private var currentRoom : Room ? = null
    private val controlMessageQueue : ConcurrentBlockingQueue<ControlMessage> = LinkedBlockingQueue()
    private val clientScope = CoroutineScope(Dispatchers.IO)
    private val lock = Mutex()
    private lateinit var writeJob: Job
    private lateinit var readJob: Job


    suspend fun start() {
        lock.withLock {
            writeJob = mainLoop()
            readJob = remoteReadLoop()
        }
    }
    /**
     * TODO: resolve racing
     */
    private suspend fun mainLoop(): Job =
        clientScope.launch{
            while (!exiting) {
                try {
                    val controlMessage = controlMessageQueue.take()
                    when (controlMessage) {
                        is ControlMessage.RoomMessage -> writeToRemote(controlMessage.Value)
                        is ControlMessage.RemoteLine -> executeCommand(controlMessage.Value)
                        is ControlMessage.RemoteInputEnded -> clientExit()
                        is ControlMessage.Stop -> serverExit()
                        else -> logger.info("Unknown message ${controlMessage}, ignoring it")
                    }
                } catch (e: Exception) {
                    logger.info("Unexpected exception while handling message: ${e.message}, ending connection")
                    exiting = true
                }
            }
    }
    /**
     * TODO: resolve racing
     */
    private suspend fun remoteReadLoop(): Job =
        clientScope.launch{
            try {
                while (!exiting) {
                    val line = clientChannel.read(5, TimeUnit.MINUTES)
                    if (line!!.isEmpty()) break
                    controlMessageQueue.add(ControlMessage.RemoteLine(line))
                }
            } catch (e: Exception) {
                // Unexpected exception, log and exit
                logger.info("Exception while waiting for connection read: ${e.message}")
            } finally {
                if (!exiting) {
                    controlMessageQueue.add(ControlMessage.RemoteInputEnded())
                }
            }
            logger.info("Exiting ReadLoop")
        }
    /**
     * TODO: resolve racing
     */
    fun postRoomMessage(message: String, sender: Room) {
        controlMessageQueue.add(ControlMessage.RoomMessage(message, sender))
    }
    /**
     * TODO: resolve racing
     */
    fun exit() {
        controlMessageQueue.add(ControlMessage.Stop())
    }

    // Synchronizes with the client termination
    suspend fun join() {
        readJob.join()
        writeJob.join()
    }

    private suspend fun postMessageToRoom(message: String) {
        if (currentRoom == null) {
            clientChannel.write("Need to be inside a room to post a message")
        } else {
            currentRoom?.post(this, message)
        }
    }

    private suspend fun writeToRemote(line: String) {
        clientChannel.write(line)
    }
    private suspend fun writeErrorToRemote(line: String) {
        writeToRemote("[Error: ${line}]")
    }
    private suspend fun writeOkToRemote() {
        writeToRemote("[OK]")
    }

    private suspend fun executeCommand(lineText: String) {
        val line = Line.parse(lineText)
        when (line){
            is Line.InvalidLine -> writeErrorToRemote(line.reason)
            is Line.Message -> postMessageToRoom(line.value)
            is Line.EnterRoomCommand -> enterRoom(line.name)
            is Line.LeaveRoomCommand -> leaveRoom()
            is Line.ExitCommand -> clientExit()
            else -> writeErrorToRemote("unable to process line")
        }
    }

    private suspend fun enterRoom(name: String) {
        currentRoom?.leave(this)
        currentRoom = rooms.getOrCreateRoom(name)
        currentRoom?.enter(this)
        writeOkToRemote();
    }

    private suspend fun leaveRoom() {
        if (currentRoom == null)
        {
            writeErrorToRemote("There is no room to leave from");
        } else {
            currentRoom?.leave(this)
            currentRoom = null
            writeOkToRemote()
        }
    }

    private suspend fun clientExit() {
        currentRoom?.leave(this)
        exiting = true
        writeOkToRemote()
    }

    private suspend fun serverExit() {
        currentRoom?.leave(this);
        exiting = true;
        writeErrorToRemote("Server is exiting");
    }

}