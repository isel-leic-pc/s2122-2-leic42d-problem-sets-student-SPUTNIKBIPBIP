package isel.leic.pc.coroutines4.servers

import java.util.concurrent.ConcurrentHashMap

class RoomSet {

    //TODO: Usar métodos computeIfAbsent
    private val rooms = ConcurrentHashMap<String, Room>()

    fun getOrCreateRoom(name: String) : Room? {
        if (rooms.contains(name))
            return rooms[name]
        val room = Room(name)
        rooms[name] = room
        return room
    }
}