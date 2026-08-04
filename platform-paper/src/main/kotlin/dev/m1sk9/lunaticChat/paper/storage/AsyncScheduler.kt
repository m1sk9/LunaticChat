package dev.m1sk9.lunaticChat.paper.storage

/**
 * Runs a task off the tick thread after a delay.
 *
 * An interface rather than the Bukkit scheduler directly so that persistence can be exercised
 * without a running server: the debounce is a rule about when writes happen, and asserting it
 * through a mocked plugin proved nothing about the rule.
 */
fun interface AsyncScheduler {
    fun runDelayed(
        delaySeconds: Long,
        task: () -> Unit,
    )
}
