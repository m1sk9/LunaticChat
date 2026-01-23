package dev.m1sk9.lunaticChat.paper.i18n

import kotlinx.serialization.Serializable

/**
 * Represents the structure of a language file.
 * This data class mirrors the YAML structure of language files (en.yml, ja.yml).
 *
 * @property commandDescription Command descriptions for various commands
 * @property directMessageNoticeStatus Status message for direct message notifications
 * @property directMessageNoticeToggle Toggle message for direct message notifications
 * @property replyTargetNotFound Error message when reply target is not found
 * @property romajiConversionStatus Status message for romaji conversion
 * @property romajiConversionToggle Toggle message for romaji conversion
 * @property tellTargetOffline Error message when tell target is offline
 * @property tellYourself Error message when trying to message yourself
 * @property toggle Toggle state messages (on/off)
 */
@Serializable
data class LanguageConfig(
    val commandDescription: CommandDescriptions,
    val directMessageNoticeStatus: String,
    val directMessageNoticeToggle: String,
    val replyTargetNotFound: String,
    val romajiConversionStatus: String,
    val romajiConversionToggle: String,
    val tellTargetOffline: String,
    val tellYourself: String,
    val toggle: ToggleMessages,
)

/**
 * Command descriptions for all LunaticChat commands.
 *
 * @property jp Description for the /jp command
 * @property notice Description for the /notice command
 * @property reply Description for the /reply command
 * @property tell Description for the /tell command
 */
@Serializable
data class CommandDescriptions(
    val jp: String,
    val notice: String,
    val reply: String,
    val tell: String,
)

/**
 * Toggle state messages.
 *
 * @property on Message for "enabled" state
 * @property off Message for "disabled" state
 */
@Serializable
data class ToggleMessages(
    val on: String,
    val off: String,
)
