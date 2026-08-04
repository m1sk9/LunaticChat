package dev.m1sk9.lunaticChat.paper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
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

    private fun createQueue(
        logger: TestUtils.TestLogger = TestUtils.TestLogger(),
        scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    ) = PerPlayerWorkQueue(scope, logger)

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
            val queue = createQueue()
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
            val queue = createQueue()
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
        val queue = createQueue()
        val started = ConcurrentLinkedQueue<String>()

        queue.submit(alice) {
            delay(2_000)
            started.add("done")
        }

        // submit returns without waiting for the work; the point of queueing off the tick thread.
        assertTrue(started.isEmpty())
    }

    @Test
    fun `work still queued when the player is released is abandoned`() =
        runBlocking {
            val queue = createQueue()
            val completed = ConcurrentLinkedQueue<String>()

            // Delivering it would spend a conversion round trip to write to a player who has left,
            // and hold both them and their recipient alive until the backlog drained.
            queue.submit(alice) {
                delay(5_000)
                completed.add("first")
            }
            queue.submit(alice) { completed.add("behind-it") }
            queue.release(alice)

            delay(200)
            assertTrue(completed.isEmpty())
        }

    @Test
    fun `a released player gets a fresh queue if they come back`() =
        runBlocking {
            val queue = createQueue()
            val completed = ConcurrentLinkedQueue<String>()

            queue.submit(alice) { completed.add("before") }
            awaitSize(completed, 1)
            queue.release(alice)

            queue.submit(alice) { completed.add("after") }

            awaitSize(completed, 2)
            assertEquals(listOf("before", "after"), completed.toList())
        }

    @Test
    fun `a failed item is reported and does not stop the player's later work`() =
        runBlocking {
            val logger = TestUtils.TestLogger()
            val queue = createQueue(logger)
            val completed = ConcurrentLinkedQueue<String>()

            // Without a guard around each item, the throw would end the consumer loop while its
            // channel stayed registered - so everything queued afterwards would be buffered and
            // never delivered, and the player would have no way out short of reconnecting.
            queue.submit(alice) { error("delivery blew up") }
            queue.submit(alice) { completed.add("after-failure") }

            awaitSize(completed, 1)
            assertEquals(listOf("after-failure"), completed.toList())
            assertTrue(logger.severeMessages.any { it.contains("Queued work failed") })
        }

    @Test
    fun `work submitted after shutdown is reported rather than silently dropped`() {
        val logger = TestUtils.TestLogger()
        val scope = CoroutineScope(Dispatchers.Default)
        val queue = createQueue(logger, scope)
        val completed = ConcurrentLinkedQueue<String>()

        scope.cancel()
        queue.submit(alice) { completed.add("never") }

        assertTrue(completed.isEmpty())
        assertTrue(logger.warningMessages.any { it.contains("full or closed") })
    }

    @Test
    fun `a player who outruns delivery is refused rather than queued without limit`() =
        runBlocking {
            val logger = TestUtils.TestLogger()
            val queue = createQueue(logger)
            val completed = ConcurrentLinkedQueue<String>()

            // The first item occupies the worker, so everything after it fills the buffer. An
            // unbounded queue accepted all of them and delivered them minutes later.
            repeat(64) { index ->
                queue.submit(alice) {
                    delay(5_000)
                    completed.add("item-$index")
                }
            }

            assertTrue(logger.warningMessages.any { it.contains("full or closed") })
            assertTrue(completed.isEmpty())
        }
}
