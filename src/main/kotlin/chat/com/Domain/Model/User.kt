package chat.com.Domain.Model

data class User(
    val nickname: Nickname
) {
    fun sendMessageToUser(message: BaseMessage) {}
}