package dev.m1sk9.lunaticChat.paper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

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
) {
    private val queues = ConcurrentHashMap<UUID, SendChannel<suspend () -> Unit>>()

    /**
     * Queues [work] behind anything already pending for [playerId].
     */
    fun submit(
        playerId: UUID,
        work: suspend () -> Unit,
    ) {
        queues.computeIfAbsent(playerId) { startWorker() }.trySend(work)
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
                work()
            }
        }
        return channel
    }
}
