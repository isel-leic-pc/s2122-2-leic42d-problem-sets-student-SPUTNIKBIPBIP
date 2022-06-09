package isel.leic.pc.coroutines4.servers


import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.ConcurrentHashMap


private abstract class ControlMessage {

    class RoomMessage(val Sender: Room, val Value: String) : ControlMessage()
    class RemoteLine(val Value: String) : ControlMessage()
    class RemoteInputEnded : ControlMessage()

    class Stop : ControlMessage() {
    }
}

private var currentRoom : Room ? = null
private val controlMessageQueue = ConcurrentHashMap.newKeySet<ControlMessage>()

class ConnectedClient(val name: String, val clientChannel: AsynchronousSocketChannel, val rooms: RoomSet) {

    private var exiting : Boolean = false


    //TODO: get access to clientChannel refactor server start
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
        controlMessageQueue.add(ControlMessage.Stop())
    }

    // Synchronizes with the client termination
    fun join() {
    }

    private suspend fun writeToRemote(line: String) {
        write(clientChannel, "Need to be inside a room to post a message")
    }

    private suspend fun writeErrorToRemote(string line) => writeToRemote($"[Error: {line}]");
    private suspend fun writeOkToRemote() => writeToRemote("[OK]");

    suspend fun executeCommand(lineText: String) {
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

    private fun enterRoom(name: String) {
        currentRoom?.leave(this)
        currentRoom = rooms.getOrCreateRoom(name)
        currentRoom?.enter(this)
        writeOkToRemote();
    }

    private fun leaveRoom() {
        if (currentRoom == null)
        {
            WriteErrorToRemote("There is no room to leave from");
        } else {
            currentRoom.leave(this)
            currentRoom = null
            WriteOkToRemote()
        }
    }

    private fun clientExit() {
        currentRoom?.leave(this)
        exiting = true
        WriteOkToRemote()
    }

    private fun serverExit() {
        currentRoom?.leave(this);
        exiting = true;
        WriteErrorToRemote("Server is exiting");
    }

}