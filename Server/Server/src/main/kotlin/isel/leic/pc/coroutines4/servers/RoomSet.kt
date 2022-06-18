package isel.leic.pc.coroutines4.servers

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class RoomSet {

    private val rooms = ConcurrentHashMap<Int,Room>(32)
    private val currentRooms = AtomicInteger(0)

    fun getOrCreateRoom(name: String) : Room {
        val roomFromMap = rooms.searchValues(2) {
                v -> if (v.name == name) v else null
        }
        if(roomFromMap != null) return roomFromMap
        val room = Room(name)
        val newRoomNumber = currentRooms.incrementAndGet()
        rooms.putIfAbsent(newRoomNumber, room)
        return room
    }
}