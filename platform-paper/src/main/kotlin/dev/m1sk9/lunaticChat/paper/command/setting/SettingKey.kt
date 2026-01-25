package dev.m1sk9.lunaticChat.paper.command.setting

/**
 * Sealed class representing all available setting keys.
 * Each setting key has a unique identifier used in command arguments.
 */
sealed class SettingKey(
    val key: String,
) {
    /**
     * Japanese romaji conversion setting
     * Command: /lc setting japanese <on|off>
     */
    data object Japanese : SettingKey("japanese")

    /**
     * Direct message notification setting
     * Command: /lc setting notice <on|off>
     */
    data object Notice : SettingKey("notice")

    /**
     * Channel message notification setting
     * Command: /lc setting chNotice <on|off>
     */
    data object ChNotice : SettingKey("chNotice")

    companion object {
        /**
         * Returns all available setting keys.
         */
        fun values(): List<SettingKey> = listOf(Japanese, Notice, ChNotice)

        /**
         * Finds a setting key by its string representation.
         * @param key The key string to search for
         * @return The matching SettingKey or null if not found
         */
        fun fromString(key: String): SettingKey? = values().find { it.key.equals(key, ignoreCase = true) }
    }
}
