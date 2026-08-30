package dev.m1sk9.lunaticChat.engine.debug

/**
 * The area of the plugin a debug line belongs to.
 *
 * Categories exist so that instrumenting the chat path - which runs once per message - does not
 * make the log unusable for someone chasing a handshake. [key] is the spelling operators write in
 * config.yml and on the Velocity command line; the enum name is never exposed to them.
 */
enum class DebugCategory(
    val key: String,
) {
    CONFIG("config"),
    CHAT("chat"),
    CHANNEL("channel"),
    CONVERSION("conversion"),
    PROTOCOL("protocol"),
    VELOCITY("velocity"),
    STORAGE("storage"),
    COMMAND("command"),
    ;

    companion object {
        private val byKey = entries.associateBy { it.key }

        /** Every [key], for the "known categories are ..." half of a message about a typo. */
        val keyList: String = entries.joinToString(", ") { it.key }

        /** The category [key] names, or null when no category is spelled that way. */
        fun fromKey(key: String): DebugCategory? = byKey[key.trim().lowercase()]
    }
}
