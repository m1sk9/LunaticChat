package dev.m1sk9.lunaticChat.paper.common

import io.ktor.client.HttpClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

/**
 * Blocks until the engine of this already-closed client has finished its own cleanup, or [timeout]
 * passes. Returns whether the cleanup finished in time.
 *
 * [HttpClient.close] only asks the CIO engine to stop: it closes its selector afterwards from a
 * coroutine on Dispatchers.IO. Once onDisable returns, Paper closes the plugin JAR, so that
 * coroutine fails with "zip file closed" the moment it needs a class not loaded yet.
 */
fun HttpClient.awaitEngineStopped(timeout: Duration): Boolean {
    val engineJob = engine.coroutineContext[Job] ?: return true
    // runBlocking is acceptable here, unlike on the tick thread: this only runs while the plugin
    // is disabling, which already saves synchronously, and the wait is bounded.
    return runBlocking { withTimeoutOrNull(timeout) { engineJob.join() } } != null
}
