package dev.m1sk9.lunaticChat.paper.command.setting

import dev.m1sk9.lunaticChat.engine.settings.PlayerChatSettings

/**
 * Sealed class representing all available setting keys.
 *
 * A setting is fully described here: the literal it is typed as, the messages that report it, and
 * how it is read from and written to [PlayerChatSettings]. [SettingHandler] is the one mechanism
 * that drives all of them.
 *
 * @property key Unique identifier used in command arguments
 * @property toggleMessageKey Language key for the message confirming a change
 * @property statusMessageKey Language key for the message reporting the current value
 */
sealed class SettingKey(
    val key: String,
    val toggleMessageKey: String,
    val statusMessageKey: String,
) {
    abstract fun read(settings: PlayerChatSettings): Boolean

    abstract fun write(
        settings: PlayerChatSettings,
        enabled: Boolean,
    ): PlayerChatSettings

    /**
     * Japanese romaji conversion setting
     * Command: /lc setting japanese <on|off>
     */
    data object Japanese : SettingKey("japanese", "romajiConversion.toggle", "romajiConversion.status") {
        override fun read(settings: PlayerChatSettings) = settings.japaneseConversionEnabled

        override fun write(
            settings: PlayerChatSettings,
            enabled: Boolean,
        ) = settings.copy(japaneseConversionEnabled = enabled)
    }

    /**
     * Direct message notification setting
     * Command: /lc setting notice <on|off>
     */
    data object Notice : SettingKey("notice", "directMessage.noticeToggle", "directMessage.noticeStatus") {
        override fun read(settings: PlayerChatSettings) = settings.directMessageNotificationEnabled

        override fun write(
            settings: PlayerChatSettings,
            enabled: Boolean,
        ) = settings.copy(directMessageNotificationEnabled = enabled)
    }

    /**
     * Channel message notification setting
     * Command: /lc setting chNotice <on|off>
     */
    data object ChNotice : SettingKey("chNotice", "channelMessage.noticeToggle", "channelMessage.noticeStatus") {
        override fun read(settings: PlayerChatSettings) = settings.channelMessageNotificationEnabled

        override fun write(
            settings: PlayerChatSettings,
            enabled: Boolean,
        ) = settings.copy(channelMessageNotificationEnabled = enabled)
    }

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
