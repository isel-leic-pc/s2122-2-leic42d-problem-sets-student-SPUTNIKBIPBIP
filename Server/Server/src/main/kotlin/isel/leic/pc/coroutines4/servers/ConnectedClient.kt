package isel.leic.pc.coroutines4.servers

import java.util.concurrent.ConcurrentHashMap


private data class Room(private val clients : ConcurrentHashMap<String, ConnectedClient>) {

}
private data class ControlMessage(private val sender : Room, val value : String)


class ConnectedClient {

    private val controlMessageQueue = NodeList<ControlMessage>()
}