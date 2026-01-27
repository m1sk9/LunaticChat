package dev.m1sk9.lunaticChat.paper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.bukkit.plugin.java.JavaPlugin
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
    private val plugin: JavaPlugin,
    private val logger: Logger,
) {
    private val job = SupervisorJob()
    val scope = CoroutineScope(Dispatchers.Default + job)

    /**
     * Cancels all coroutines in this scope.
     * Should be called during plugin disable.
     */
    fun cancel() {
        logger.info("Cancelling plugin coroutine scope...")
        scope.cancel()
        logger.info("Plugin coroutine scope cancelled.")
    }

    /**
     * Returns true if the scope is still active.
     */
    fun isActive(): Boolean = job.isActive
}
