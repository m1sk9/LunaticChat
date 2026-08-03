package dev.m1sk9.lunaticChat.paper

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Provides a coroutine scope tied to the plugin lifecycle.
 *
 * This scope:
 * - Uses Dispatchers.Default for background CPU-bound work
 * - Uses SupervisorJob to prevent child failures from canceling the entire scope
 * - Is properly cancelled when the plugin disables
 *
 * Usage:
 * ```kotlin
 * pluginScope.launch {
 *     val result = withTimeout(5000) {
 *         someAsyncOperation()
 *     }
 *     // Handle result...
 * }
 * ```
 */
class PluginCoroutineScope(
    private val logger: Logger,
) {
    private val job = SupervisorJob()

    // Without this, a coroutine that throws reports to the JVM default handler and never reaches
    // the plugin's log - and callers that dispatch work and return immediately have no other way
    // to learn that it failed.
    private val errorHandler =
        CoroutineExceptionHandler { _, throwable ->
            logger.log(Level.SEVERE, "Unhandled exception in a plugin coroutine", throwable)
        }

    val scope = CoroutineScope(Dispatchers.Default + job + errorHandler)

    /**
     * Cancels all coroutines in this scope.
     * Should be called during plugin disable.
     */
    fun cancel() {
        logger.info("Cancelling plugin coroutine scope...")
        scope.cancel()
        logger.info("Plugin coroutine scope cancelled.")
    }
}
