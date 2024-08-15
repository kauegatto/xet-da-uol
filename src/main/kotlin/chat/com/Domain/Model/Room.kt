package chat.com.Domain.Model

import kotlinx.coroutines.flow.MutableSharedFlow

data class Room(
    val roomId: String,
    val messageFlow: MutableSharedFlow<Message> = MutableSharedFlow(replay = 0)
) {
    init {
        rooms[roomId] = this
    }

    val users: MutableMap<Nickname, User> = mutableMapOf()

    fun addUser(user: User) {
        users[user.nickname] = user
    }

    fun removeUser(nickname: Nickname) {
        users.remove(nickname)
    }

    suspend fun broadcastMessage(message: Message) {
        messageFlow.emit(message)
    }

    fun isUserOnRoom(userId: Nickname): Boolean {
        return this.users.containsKey(userId)
    }
    
    
    companion object {
        private val rooms: MutableMap<String, Room> = mutableMapOf()
        fun getById(roomId: String): Room? = rooms[roomId]
        fun isUserOnRoom(roomId: String, userId: Nickname): Boolean = getById(roomId)?.users?.containsKey(userId) ?: false
        fun getOrCreateRoom(roomId: String) = rooms.computeIfAbsent(roomId) { Room(roomId) }
        fun exists(roomId: String) = rooms.containsKey(roomId)
    }
}
