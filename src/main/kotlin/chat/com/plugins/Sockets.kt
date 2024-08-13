package chat.com.plugins

import chat.com.Model.Message
import io.ktor.serialization.*
import io.ktor.serialization.kotlinx.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import java.time.Duration

fun Application.configureSockets() {
    val jsonConverter = KotlinxWebsocketSerializationConverter(Json)
    install(WebSockets) {
        contentConverter = jsonConverter
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        webSocket("/ws") { // websocketSession
            for (frame in incoming) {
                val message = jsonConverter.deserialize<Message>(frame)
                val frame: Frame = jsonConverter.serialize(message)
                outgoing.send(frame)
            }
        }
    }
}
