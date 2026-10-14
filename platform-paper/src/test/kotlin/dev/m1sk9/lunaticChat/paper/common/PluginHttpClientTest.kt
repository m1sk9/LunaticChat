package dev.m1sk9.lunaticChat.paper.common

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class PluginHttpClientTest {
    // Joined rather than checked with isAlive alone: the pool reports termination from inside its
    // last worker, a moment before that thread actually exits.
    private fun liveHttpThreads(): List<Thread> =
        Thread
            .getAllStackTraces()
            .keys
            .filter { it.name.startsWith("LunaticChat-http-") }
            .onEach { it.join(1_000) }
            .filter { it.isAlive }

    @Test
    fun `Ktor's cleanup has run and no client thread is left once close returns`() {
        repeat(20) {
            val http = PluginHttpClient()

            assertTrue(http.close(5.seconds))
            assertEquals(0, http.discardedTasks)
            assertEquals(emptyList(), liveHttpThreads())
        }
    }

    @Test
    fun `Ktor's cleanup has run and no client thread is left once close returns after a request`() {
        ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use { server ->
            val responder =
                thread {
                    server.accept().use { socket ->
                        socket.getInputStream().bufferedReader().let { reader ->
                            while (!reader.readLine().isNullOrEmpty()) Unit
                        }
                        socket.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 2\r\n\r\nok".toByteArray())
                    }
                }
            val http = PluginHttpClient()

            val body = runBlocking { http.client.get("http://127.0.0.1:${server.localPort}/").bodyAsText() }

            assertEquals("ok", body)
            assertTrue(http.close(5.seconds))
            assertEquals(0, http.discardedTasks)
            assertEquals(emptyList(), liveHttpThreads())
            responder.join()
        }
    }
}
