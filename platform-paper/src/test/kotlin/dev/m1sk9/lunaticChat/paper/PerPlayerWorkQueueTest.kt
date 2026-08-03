package dev.m1sk9.lunaticChat.paper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PerPlayerWorkQueueTest {
    private val alice = UUID.fromString("00000001-0000-0000-0000-000000000000")
    private val bob = UUID.fromString("00000002-0000-0000-0000-000000000000")

    private suspend fun awaitSize(
        completed: ConcurrentLinkedQueue<String>,
        expected: Int,
    ) = withTimeout(5_000) {
        while (completed.size < expected) {
            delay(5)
        }
    }

    @Test
    fun `a player's work runs in submission order even when later work is faster`() =
        runBlocking {
            val queue = PerPlayerWorkQueue(CoroutineScope(Dispatchers.Default))
            val completed = ConcurrentLinkedQueue<String>()

            // The regression this guards: a cached conversion overtaking an uncached one sent
            // before it, so the player sees their messages out of order.
            queue.submit(alice) {
                delay(200)
                completed.add("slow-first")
            }
            queue.submit(alice) { completed.add("fast-second") }

            awaitSize(completed, 2)
            assertEquals(listOf("slow-first", "fast-second"), completed.toList())
        }

    @Test
    fun `one player's slow work does not hold up another player`() =
        runBlocking {
            val queue = PerPlayerWorkQueue(CoroutineScope(Dispatchers.Default))
            val completed = ConcurrentLinkedQueue<String>()

            queue.submit(alice) {
                delay(1_000)
                completed.add("alice")
            }
            queue.submit(bob) { completed.add("bob") }

            awaitSize(completed, 1)
            assertEquals(listOf("bob"), completed.toList())
        }

    @Test
    fun `submit does not block the caller`() {
        val queue = PerPlayerWorkQueue(CoroutineScope(Dispatchers.Default))
        val started = ConcurrentLinkedQueue<String>()

        queue.submit(alice) {
            delay(2_000)
            started.add("done")
        }

        // submit returns without waiting for the work; the point of queueing off the tick thread.
        assertTrue(started.isEmpty())
    }

    @Test
    fun `work already queued still runs after the player is released`() =
        runBlocking {
            val queue = PerPlayerWorkQueue(CoroutineScope(Dispatchers.Default))
            val completed = ConcurrentLinkedQueue<String>()

            queue.submit(alice) {
                delay(50)
                completed.add("in-flight")
            }
            queue.release(alice)

            awaitSize(completed, 1)
            assertEquals(listOf("in-flight"), completed.toList())
        }

    @Test
    fun `a released player gets a fresh queue if they come back`() =
        runBlocking {
            val queue = PerPlayerWorkQueue(CoroutineScope(Dispatchers.Default))
            val completed = ConcurrentLinkedQueue<String>()

            queue.submit(alice) { completed.add("before") }
            awaitSize(completed, 1)
            queue.release(alice)

            queue.submit(alice) { completed.add("after") }

            awaitSize(completed, 2)
            assertEquals(listOf("before", "after"), completed.toList())
        }
}
