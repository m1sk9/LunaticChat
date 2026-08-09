package dev.m1sk9.lunaticChat.paper.config

import dev.m1sk9.lunaticChat.paper.config.key.MessageFormatConfig

/**
 * The message formats currently in effect, which `/lc reload` may replace while the server runs.
 *
 * Deliberately narrower than [LunaticChatConfiguration]: nothing else in config.yml can be applied
 * without a restart, because the services that read it capture their values when they are built,
 * and neither commands, event listeners nor scheduled tasks can be re-registered at runtime. A
 * holder over the whole configuration would let a reload swap only part of it, leaving a tree that
 * matches no version of the file, and would blur which values a reader is allowed to trust. Taking
 * this type says the value is live; taking [LunaticChatConfiguration] says it was frozen at startup.
 */
class MessageFormatHolder(
    initial: MessageFormatConfig,
) {
    @Volatile
    var current: MessageFormatConfig = initial
        private set

    fun replace(next: MessageFormatConfig) {
        current = next
    }
}
