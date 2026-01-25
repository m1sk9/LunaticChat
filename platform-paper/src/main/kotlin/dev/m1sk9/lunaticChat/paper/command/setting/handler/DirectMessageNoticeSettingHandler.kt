package dev.m1sk9.lunaticChat.paper.command.setting.handler

import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.command.setting.SettingHandler
import dev.m1sk9.lunaticChat.paper.command.setting.SettingKey
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.i18n.MessageFormatter
import dev.m1sk9.lunaticChat.paper.settings.PlayerSettingsManager

/**
 * Handles the direct message notification setting.
 * Manages enabling/disabling DM notifications for players.
 */
class DirectMessageNoticeSettingHandler(
    private val settingsManager: PlayerSettingsManager,
    private val languageManager: LanguageManager,
) : SettingHandler {
    override val key: SettingKey = SettingKey.Notice

    override fun execute(
        ctx: CommandContext,
        enable: Boolean,
    ): CommandResult {
        val player = ctx.requirePlayer()
        val currentSettings = settingsManager.getSettings(player.uniqueId)
        val updatedSettings = currentSettings.copy(directMessageNotificationEnabled = enable)
        settingsManager.updateSettings(updatedSettings)

        val toggleText = languageManager.getToggleText(enable)
        val message =
            MessageFormatter.formatSuccess(
                languageManager.getMessage("directMessage.noticeToggle", mapOf("toggle" to toggleText)),
            )

        player.sendMessage(message)
        return CommandResult.Success
    }

    override fun showStatus(ctx: CommandContext): CommandResult {
        val player = ctx.requirePlayer()
        val settings = settingsManager.getSettings(player.uniqueId)

        val toggleText = languageManager.getToggleText(settings.directMessageNotificationEnabled)
        val message =
            MessageFormatter.format(
                languageManager.getMessage("directMessage.noticeStatus", mapOf("toggle" to toggleText)),
            )

        player.sendMessage(message)
        return CommandResult.Success
    }
}
