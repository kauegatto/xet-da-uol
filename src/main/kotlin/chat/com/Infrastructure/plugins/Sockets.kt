package chat.com.Infrastructure.plugins

import chat.com.Domain.Model.*
import io.ktor.serialization.*
import io.ktor.serialization.kotlinx.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.Duration

fun Application.configureSockets(){
    val jsonConverter = KotlinxWebsocketSerializationConverter(Json {
        classDiscriminator = "type" // Specifies the discriminator field for polymorphic messages
    })

    install(WebSockets) {
        contentConverter = jsonConverter
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        // https://github.com/ktorio/ktor-documentation/tree/2.3.12/codeSnippets/snippets/server-websockets-sharedflow
        webSocket("/ws") { // websocketSession
            for (frame in incoming) {
                runCatching {
                    val baseMessage = jsonConverter.deserialize<BaseMessage>(frame)
                    when (baseMessage) {
                        is JoinMessage -> handleJoinMessage(baseMessage)
                        is Message -> handleMessage(baseMessage)
                    }
                }.onFailure {
                    send("Invalid message type")
                }
            }
        }
    }
}

private suspend fun DefaultWebSocketServerSession.handleMessage(
    baseMessage: Message
) {
    val room = Room.getById(baseMessage.roomId)
    if(room == null) {
        send("Room of id ${baseMessage.roomId} not found!");
        return
    }
    if (room.isUserOnRoom(baseMessage.sender)) {
        send("User ${baseMessage.sender} was not found on room ${baseMessage.roomId}")
    }
    room.broadcastMessage(baseMessage)
}

private suspend fun DefaultWebSocketServerSession.handleJoinMessage(
    message: JoinMessage,
) {
    val roomId = message.roomId
    val connectedRoom = Room.getOrCreateRoom(roomId)
    connectedRoom.addUser(
        User(
            nickname = message.nickname
        )
    )
    startListeningToRoomFlow(connectedRoom)
    send("Connected to room $roomId")
}

private fun DefaultWebSocketServerSession.startListeningToRoomFlow(connectedRoom: Room) = launch {
    connectedRoom.messageFlow.collect { message ->
        sendSerialized(message)
    }
}
