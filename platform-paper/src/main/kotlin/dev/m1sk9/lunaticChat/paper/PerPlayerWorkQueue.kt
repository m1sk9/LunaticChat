package dev.m1sk9.lunaticChat.paper

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
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
    private val queues = ConcurrentHashMap<UUID, SendChannel<suspend () -> Unit>>()

    /**
     * Queues [work] behind anything already pending for [playerId].
     */
    fun submit(
        playerId: UUID,
        work: suspend () -> Unit,
    ) {
        // Checked rather than left to trySend: cancelling the scope kills the worker coroutines but
        // does not close their channels, so after shutdown trySend would keep reporting success for
        // work nothing will ever read.
        val accepted = scope.isActive && queues.computeIfAbsent(playerId) { startWorker() }.trySend(work).isSuccess
        if (!accepted) {
            // Reachable once the scope is cancelled at shutdown, or if the player's queue is
            // released in the same tick as their command. Dropping a message in silence is worse
            // than saying so.
            logger.warning("Discarded queued work for player $playerId: their queue is closed")
        }
    }

    /**
     * Drops the player's queue once they can no longer send anything. Work already queued still
     * runs; without this the map and its worker coroutines would grow for the life of the server.
     */
    fun release(playerId: UUID) {
        queues.remove(playerId)?.close()
    }

    private fun startWorker(): SendChannel<suspend () -> Unit> {
        // Unlimited so that submit never suspends or drops work on the caller's thread.
        val channel = Channel<suspend () -> Unit>(Channel.UNLIMITED)
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
        return channel
    }
}
