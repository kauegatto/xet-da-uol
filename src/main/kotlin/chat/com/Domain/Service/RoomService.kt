package chat.com.Domain.Service

import chat.com.Domain.Model.Nickname
import chat.com.Domain.Model.Room

class RoomService(private val rooms: MutableMap<String, Room>) {
    fun getOrCreateRoom(roomId: String): Room {
        return rooms.computeIfAbsent(roomId) { Room(roomId) }
    }
    fun exists(roomId: String): Boolean {
        return rooms.containsKey(roomId)
    }
    fun isUserOnRoom(roomId: String, userId: Nickname): Boolean {
        return rooms.containsKey(roomId)
    }
}
