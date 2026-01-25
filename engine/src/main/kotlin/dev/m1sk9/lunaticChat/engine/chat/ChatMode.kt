package dev.m1sk9.lunaticChat.engine.chat

/**
 * Represents the chat mode for a player.
 *
 * Chat mode determines where messages are sent by default:
 * - GLOBAL: All messages go to global chat (visible to everyone)
 * - CHANNEL: All messages go to the active channel (visible to channel members only)
 *
 * Players can temporarily override their mode by prefixing messages with '!'.
 * Chat mode is persisted and maintains its state across server restarts.
 * Default mode is GLOBAL.
 */
enum class ChatMode {
    /**
     * Global chat mode - messages visible to all players.
     */
    GLOBAL,

    /**
     * Channel chat mode - messages visible only to channel members.
     * Requires player to be in an active channel.
     */
    CHANNEL,
    ;

    /**
     * Returns the opposite chat mode.
     * Used when player prefixes message with '!'.
     */
    fun toggle(): ChatMode =
        when (this) {
            GLOBAL -> CHANNEL
            CHANNEL -> GLOBAL
        }

    companion object {
        /**
         * Default chat mode for all players.
         */
        val DEFAULT = GLOBAL
    }
}
