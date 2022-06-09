package isel.leic.pc.coroutines4.servers

import java.util.concurrent.ConcurrentHashMap

class Room(name : String) {

    private val clients = ConcurrentHashMap.newKeySet<ConnectedClient>()

    fun enter(client : ConnectedClient) {
        clients.add(client)
    }

    fun leave(client: ConnectedClient) {
        clients.remove(client)
    }

    suspend fun post(client: ConnectedClient, message: String) {
        val formattedMessage  = "Name: ${client.name} says ${message}"
        clients.forEach { receiver ->
            if (receiver != client)
                receiver.postRoomMessage(formattedMessage, this)
        }
    }
}
