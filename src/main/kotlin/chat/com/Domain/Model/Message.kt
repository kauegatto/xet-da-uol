package chat.com.Domain.Model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias Nickname = String

@Serializable
sealed class BaseMessage { abstract val type: String }

@Serializable
@SerialName("join")
data class JoinMessage(
    override val type: String,
    val roomId: String,
    val nickname: String
) : BaseMessage()

@Serializable
@SerialName("chat")
data class Message(
    override val type: String,
    val roomId: String,
    val sender: String,
    val content: String
) : BaseMessage()