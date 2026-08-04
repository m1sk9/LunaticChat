package dev.m1sk9.lunaticChat.paper

/**
 * A service with work to finish before the server stops - a cache to flush, a connection to close.
 *
 * Implementing this is how a service gets torn down: [ServiceInitializer] registers each one as it
 * builds it, so shutdown follows from construction rather than from a second hand-maintained list
 * that a new service is silently missing from.
 *
 * The five services that had teardown before this spelled it `saveToDisk()` three times and
 * `shutdown()` twice, so nothing but a reader could tell they were the same obligation.
 */
interface StoppableService {
    /** Finishes outstanding work. Called once, on the shutdown path, off the tick thread. */
    fun stop()
}
