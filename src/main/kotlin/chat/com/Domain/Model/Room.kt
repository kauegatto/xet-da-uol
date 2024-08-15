package chat.com.Domain.Model

import kotlinx.coroutines.flow.MutableSharedFlow

data class Room(
    val roomId: String,
    val messageFlow: MutableSharedFlow<Message> = MutableSharedFlow(replay = 0)
) {
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
}
