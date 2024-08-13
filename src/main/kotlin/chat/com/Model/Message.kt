package chat.com.Model

import kotlinx.serialization.Serializable

typealias Nickname = String

@Serializable
data class Message(
    val content: String,
    val sender: Nickname,
)