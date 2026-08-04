package dev.m1sk9.lunaticChat.paper

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Runs work submitted for a player one item at a time, in the order it was submitted.
 *
 * Launching a coroutine per message would let a fast one overtake a slow one - a cached romaji
 * conversion finishing ahead of an uncached one sent before it - so a player's messages could
 * appear out of order to themselves and to their recipient. A queue per player keeps each player's
 * messages ordered while still letting different players proceed independently.
 *
 * [submit] must be called from one thread per player (the server's command thread), since that is
 * what makes "the order it was submitted" well defined.
 */
class PerPlayerWorkQueue(
    private val scope: CoroutineScope,
    private val logger: Logger,
) {
    private val queues = ConcurrentHashMap<UUID, Worker>()

    private companion object {
        /**
         * Bounded so a player cannot make the server hold work it will never get through.
         *
         * An item takes up to the conversion timeout to drain, which is far slower than a player can
         * send; an unbounded queue therefore grew for as long as a macro ran, each item holding its
         * sender and recipient alive and arriving minutes after it was typed. Refusing the overflow
         * is visible to the player, unlike delivering it late.
         */
        const val CAPACITY = 8
    }

    private class Worker(
        val channel: SendChannel<suspend () -> Unit>,
        val job: Job,
    )

    /**
     * Queues [work] behind anything already pending for [playerId], unless their queue is full.
     */
    fun submit(
        playerId: UUID,
        work: suspend () -> Unit,
    ) {
        // Checked rather than left to trySend: cancelling the scope kills the worker coroutines but
        // does not close their channels, so after shutdown trySend would keep reporting success for
        // work nothing will ever read.
        val accepted =
            scope.isActive &&
                queues
                    .computeIfAbsent(playerId) { startWorker() }
                    .channel
                    .trySend(work)
                    .isSuccess
        if (!accepted) {
            // Reachable when the player is sending faster than delivery drains, once the scope is
            // cancelled at shutdown, or if their queue is released in the same tick as their command.
            // Dropping a message in silence is worse than saying so.
            logger.warning("Discarded queued work for player $playerId: their queue is full or closed")
        }
    }

    /**
     * Drops the player's queue once they can no longer send anything.
     *
     * Anything still pending is abandoned rather than delivered: it was addressed to or sent by a
     * player who has left, so finishing it would spend a conversion round trip per item to write to
     * nobody, while holding both players alive until the backlog drained.
     */
    fun release(playerId: UUID) {
        queues.remove(playerId)?.let {
            it.channel.close()
            it.job.cancel()
        }
    }

    private fun startWorker(): Worker {
        val channel = Channel<suspend () -> Unit>(CAPACITY)
        val job =
            scope.launch {
                for (work in channel) {
                    // One failed message must not end the loop. If it did, the channel would stay in
                    // `queues` with nothing reading it, so every later message from this player would
                    // be buffered and never delivered - with no way to recover short of reconnecting.
                    try {
                        work()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        logger.log(Level.SEVERE, "Queued work failed", e)
                    }
                }
            }
        return Worker(channel, job)
    }
}
