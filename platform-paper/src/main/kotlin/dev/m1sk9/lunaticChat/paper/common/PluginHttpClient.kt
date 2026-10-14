package dev.m1sk9.lunaticChat.paper.common

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executor
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * A CIO [HttpClient] whose engine runs only on threads this plugin owns, so that [close] can wait
 * for all of its work to finish.
 *
 * Ktor finishes closing the engine asynchronously - it closes the selector and ends the selector
 * loop after [HttpClient.close] has returned. On Dispatchers.IO that work could outlive onDisable,
 * and once Paper has closed the plugin JAR it fails with "zip file closed" as soon as it needs a
 * class not loaded yet. Neither piece is a child of a job the client exposes, so joining a job
 * cannot cover them; owning the threads can.
 */
class PluginHttpClient(
    threads: Int = DEFAULT_THREADS,
) {
    private companion object {
        // One thread can be held for up to 500 ms by the selector loop's blocking select, so a
        // single thread would stall requests behind it.
        const val DEFAULT_THREADS = 2
    }

    private val threadIds = AtomicInteger()
    private val discarded = AtomicInteger()

    /** Tasks dropped because they arrived after [close] shut the pool down. */
    internal val discardedTasks: Int get() = discarded.get()

    private val pool =
        ThreadPoolExecutor(
            threads,
            threads,
            0L,
            TimeUnit.MILLISECONDS,
            LinkedBlockingQueue(),
            { task ->
                Thread(task, "LunaticChat-http-${threadIds.incrementAndGet()}").apply { isDaemon = true }
            },
            // Discard rather than throw: on rejection kotlinx-coroutines moves the task to
            // Dispatchers.IO, which is exactly where it must not run once the JAR is closed.
            { _, _ -> discarded.incrementAndGet() },
        )

    private val tracker = InFlightTracker(pool)

    val client: HttpClient =
        HttpClient(CIO) {
            engine { dispatcher = tracker.asCoroutineDispatcher() }
        }

    /**
     * Closes the client and blocks until its threads have stopped, or [timeout] passes. Returns
     * whether they stopped in time.
     */
    fun close(timeout: Duration): Boolean {
        val deadline = TimeSource.Monotonic.markNow() + timeout
        client.close()
        // The pool is shut down only once Ktor's cleanup has drained, because a task it submits
        // after shutdown is discarded - which would leave the selector open.
        val drained = tracker.awaitIdle(deadline)
        pool.shutdown()
        val remaining = (-deadline.elapsedNow()).coerceAtLeast(Duration.ZERO)
        return drained && pool.awaitTermination(remaining.inWholeMilliseconds, TimeUnit.MILLISECONDS)
    }

    /**
     * Counts tasks from submission until they finish running.
     *
     * The pool's own active count and queue are not enough to tell when it is idle: a worker that
     * has taken a task off the queue but not yet started it shows up in neither. A task here stays
     * counted until it has run, and any task it dispatches is counted before it finishes, so the
     * count reaches zero only once the chain of work has ended.
     */
    private class InFlightTracker(
        private val delegate: Executor,
    ) : Executor {
        private val lock = Object()
        private var inFlight = 0

        override fun execute(command: Runnable) {
            synchronized(lock) { inFlight++ }
            delegate.execute {
                try {
                    command.run()
                } finally {
                    finished()
                }
            }
        }

        private fun finished() {
            synchronized(lock) {
                inFlight--
                if (inFlight == 0) lock.notifyAll()
            }
        }

        fun awaitIdle(deadline: TimeSource.Monotonic.ValueTimeMark): Boolean =
            synchronized(lock) {
                while (inFlight > 0) {
                    val remaining = (-deadline.elapsedNow()).inWholeMilliseconds
                    if (remaining <= 0) return false
                    lock.wait(remaining)
                }
                true
            }
    }
}
