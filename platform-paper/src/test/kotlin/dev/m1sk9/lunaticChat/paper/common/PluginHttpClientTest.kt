package dev.m1sk9.lunaticChat.paper.common

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
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

    @Test
    fun `close reports failure when work on the client's threads outlives the timeout`() {
        val http = PluginHttpClient()
        val release = CountDownLatch(1)
        val finished = CountDownLatch(1)
        CoroutineScope(http.client.engine.dispatcher).launch {
            try {
                release.await()
            } finally {
                finished.countDown()
            }
        }

        try {
            assertFalse(http.close(50.milliseconds))
        } finally {
            release.countDown()
            finished.await(5, TimeUnit.SECONDS)
        }
    }

    @Test
    fun `work dispatched after close is discarded rather than run elsewhere`() {
        val http = PluginHttpClient()
        assertTrue(http.close(5.seconds))
        val ran = AtomicBoolean(false)

        CoroutineScope(http.client.engine.dispatcher).launch { ran.set(true) }

        assertEquals(1, http.discardedTasks)
        // kotlinx-coroutines would rerun a rejected task on Dispatchers.IO; give it the chance to.
        Thread.sleep(200)
        assertFalse(ran.get())
    }
}
