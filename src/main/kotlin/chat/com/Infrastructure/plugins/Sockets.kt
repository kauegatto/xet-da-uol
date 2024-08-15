package chat.com.Infrastructure.plugins

import chat.com.Domain.Model.*
import chat.com.Domain.Service.RoomService
import io.ktor.serialization.*
import io.ktor.serialization.kotlinx.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.Duration

fun Application.configureSockets(roomService: RoomService) {
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
                        is JoinMessage -> handleJoinMessage(baseMessage, roomService)
                        is Message -> handleMessage(roomService, baseMessage)
                    }
                }.onFailure {
                    send("Invalid message type")
                }
            }
        }
    }
}

private suspend fun DefaultWebSocketServerSession.handleMessage(
    roomService: RoomService,
    baseMessage: Message
) {
    if (!roomService.exists(baseMessage.roomId)) {
        send("Room ${baseMessage.roomId} was not found")
    }
    if (!roomService.isUserOnRoom(baseMessage.roomId, baseMessage.sender)) {
        send("User ${baseMessage.sender} was not found on room ${baseMessage.roomId}")
    }
    roomService.getOrCreateRoom(baseMessage.roomId).broadcastMessage(baseMessage)
}

private suspend fun DefaultWebSocketServerSession.handleJoinMessage(
    message: JoinMessage,
    roomService: RoomService,
) {
    val roomId = message.roomId
    val connectedRoom = roomService.getOrCreateRoom(roomId)
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
