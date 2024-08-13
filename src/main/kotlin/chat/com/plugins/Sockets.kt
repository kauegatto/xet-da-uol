package chat.com.plugins

import chat.com.Model.*
import io.ktor.serialization.*
import io.ktor.serialization.kotlinx.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.time.Duration
import java.util.*
import kotlin.collections.HashMap

fun Application.configureSockets() {
    val jsonConverter = KotlinxWebsocketSerializationConverter(Json {
        classDiscriminator = "type" // Specifies the discriminator field for polymorphic messages
    })

    val rooms = Collections.synchronizedMap<String, Room>(HashMap())
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
                        is JoinMessage -> handleJoinMessage(baseMessage, rooms)
                        is Message -> handleMessage(rooms, baseMessage)
                    }
                }.onFailure {
                    send("Invalid message type")
                }
            }
        }
    }
}

private suspend fun DefaultWebSocketServerSession.handleMessage(
    rooms: MutableMap<String, Room>,
    baseMessage: Message
) {
    if (!rooms.containsKey(baseMessage.roomId)) {
        send("Room ${baseMessage.roomId} was not found")
    }
    if (!rooms[baseMessage.roomId]!!.userToSessions.containsKey(baseMessage.sender)) {
        send("User ${baseMessage.sender} was not found on room ${baseMessage.roomId}")
    }
    rooms[baseMessage.roomId]!!.messageFlow.emit(baseMessage)
}

private suspend fun DefaultWebSocketServerSession.handleJoinMessage(
    message: JoinMessage,
    rooms: MutableMap<String, Room>,
) {
    val roomId = message.roomId
    if (!rooms.containsKey(roomId)) {
        createRoom(rooms, roomId)
    }

    val connectedRoom = rooms[roomId]!!
    connectedRoom.userToSessions[message.nickname] = this
    startListeningToRoomFlow(connectedRoom)
    send("Connected to room $roomId")
}

private fun createRoom(rooms: MutableMap<String, Room>, roomId: String) {
    rooms[roomId] = Room(roomId, mutableMapOf())
    println("Room $roomId created")
}

private fun DefaultWebSocketServerSession.startListeningToRoomFlow(connectedRoom: Room) = launch {
    connectedRoom.messageFlow.collect { message ->
        sendSerialized(message)
    }
}
