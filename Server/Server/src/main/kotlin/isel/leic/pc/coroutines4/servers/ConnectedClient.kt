package isel.leic.pc.coroutines4.servers


import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock


private abstract class ControlMessage {

    class RoomMessage(val Sender: Room, val Value: String) : ControlMessage()
    class RemoteLine(val Value: String) : ControlMessage()
    class RemoteInputEnded : ControlMessage()
    class Stop : ControlMessage()
}

class ConnectedClient(
    val name: String,
    val clientChannel: AsynchronousSocketChannel,
    val rooms: RoomSet
) {



    private var exiting : Boolean = false
    private val logger = KotlinLogging.logger {}
    private var currentRoom : Room ? = null
    @Volatile
    private var currentMessageNumber : Int = 0
    private val controlMessageQueue : ConcurrentHashMap<Int, ControlMessage> = ConcurrentHashMap()
    private val lock = ReentrantLock()
    suspend fun mainLoop() {
        while (!exiting) {
            try {
                //todo: if control message is empty -> delay
                var controlMessage = controlMessageQueue[currentMessageNumber--]
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

    suspend fun remoteReadLoop() {
        try {
            while (!exiting) {
                var line = read(clientChannel)
                print(line)
                controlMessageQueue.putIfAbsent(currentMessageNumber++, ControlMessage.RemoteLine(line))
            }
        } catch (e: Exception) {
            // Unexpected exception, log and exit
            logger.info("Exception while waiting for connection read: ${e.message}")
        } finally {
            if (!exiting) {
                controlMessageQueue.putIfAbsent(currentMessageNumber++, ControlMessage.RemoteInputEnded())
            }
        }
        logger.info("Exiting ReadLoop")
    }
    suspend fun postRoomMessage(formattedMessage: String, room: Room) {
        if (currentRoom != null) {
            write(clientChannel, "Need to be inside a room to post a message")
        } else {
            currentRoom!!.post(this, formattedMessage)
        }
    }

    private suspend fun postMessageToRoom(message: String) {
        if (currentRoom == null) {
            write(clientChannel, "Need to be inside a room to post a message")
        } else {
            currentRoom?.post(this, message)
        }
    }

    fun exit() {
        controlMessageQueue.putIfAbsent(currentMessageNumber++, ControlMessage.Stop())
    }

    // Synchronizes with the client termination
    fun join() {

    }

    private suspend fun writeToRemote(line: String) {
        write(clientChannel, line)
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