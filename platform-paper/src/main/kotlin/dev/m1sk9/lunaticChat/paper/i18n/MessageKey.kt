package dev.m1sk9.lunaticChat.paper.i18n

/**
 * Type-safe message keys for LunaticChat i18n system.
 *
 * This sealed class ensures compile-time safety when referencing messages,
 * preventing typos and missing translations. Each message key can have
 * associated parameters that will be substituted in the final message.
 */
sealed class MessageKey {
    // Command descriptions
    data object CommandDescriptionTell : MessageKey()

    data object CommandDescriptionReply : MessageKey()

    data object CommandDescriptionJp : MessageKey()

    data object CommandDescriptionNotice : MessageKey()

    // Toggle messages (with placeholder)
    data class DirectMessageNoticeStatus(
        val toggle: String,
    ) : MessageKey()

    data class DirectMessageNoticeToggle(
        val toggle: String,
    ) : MessageKey()

    data class RomajiConversionStatus(
        val toggle: String,
    ) : MessageKey()

    data class RomajiConversionToggle(
        val toggle: String,
    ) : MessageKey()

    // Error messages
    data object ReplyTargetNotFound : MessageKey()

    data class TellTargetOffline(
        val targetName: String,
    ) : MessageKey()

    data object TellYourself : MessageKey()

    // Toggle values
    data object ToggleOn : MessageKey()

    data object ToggleOff : MessageKey()

    // System messages
    data object PlayerOnlyCommand : MessageKey()
}
