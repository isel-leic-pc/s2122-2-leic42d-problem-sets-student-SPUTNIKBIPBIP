package isel.leic.pc.coroutines4.servers


import kotlinx.coroutines.*


private var currentRoom : Room ? = null

class ConnectedClient(val name: String, val rooms: RoomSet) {

    private fun writeToRemote(line: String) {
    }

    //TODO: get access to clientChannel refactor server start
    suspend fun postRoomMessage(formattedMessage: String, room: Room) {
        if (currentRoom != null) {
            write(clientChannel, "Need to be inside a room to post a message")
        } else {
            currentRoom!!.post(this, formattedMessage)
        }
    }

    private abstract class ControlMessage {

        class RoomMessage(val Sender: Room, val Value: String) : ControlMessage()
        class RemoteLine(val Value: String) : ControlMessage()
        class RemoteInputEnded : ControlMessage()

        class Stop : ControlMessage() {
        }
    }
}