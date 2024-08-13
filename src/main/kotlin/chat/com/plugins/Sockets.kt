package chat.com.plugins

import chat.com.Model.Message
import chat.com.Model.Nickname
import io.ktor.serialization.*
import io.ktor.serialization.kotlinx.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.Duration
import java.util.*

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
        val messageResponseFlow = MutableSharedFlow<Message>()
        val sharedFlow = messageResponseFlow.asSharedFlow()

        // https://github.com/ktorio/ktor-documentation/tree/2.3.12/codeSnippets/snippets/server-websockets-sharedflow
        webSocket("/ws") { // websocketSession
            val job = launch {
                sharedFlow.collect { message ->
                    sendSerialized(message)
                }
            }
            incoming.consumeEach { frame ->
                val message = jsonConverter.deserialize<Message>(frame)
                messageResponseFlow.emit(message)
            } // could also runCatching
            job.cancel()
        }
    }
}
