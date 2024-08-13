package example.com.plugins

import io.ktor.server.application.*

fun Application.configureSerialization() {
    install(ContentNegotiation) {
    }
}
