package chat.com.Model

import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableSharedFlow

data class Room (
    val roomId: String,
    val userToSessions: MutableMap<Nickname, WebSocketSession> = mutableMapOf(),
    val messageFlow: MutableSharedFlow<Message> = MutableSharedFlow(replay = 0)
)