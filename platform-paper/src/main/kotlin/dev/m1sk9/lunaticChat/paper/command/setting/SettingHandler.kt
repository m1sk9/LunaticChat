package dev.m1sk9.lunaticChat.paper.command.setting

import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.i18n.MessageFormatter
import dev.m1sk9.lunaticChat.paper.settings.PlayerSettingsManager

/**
 * Reads and writes one player setting, identified by [key].
 *
 * Everything that differs between settings lives on the [SettingKey]; this class is the shared
 * mechanism that applies it.
 */
class SettingHandler(
    val key: SettingKey,
    private val settingsManager: PlayerSettingsManager,
    private val languageManager: LanguageManager,
) {
    /**
     * Enables or disables the setting for a player.
     *
     * @param ctx The command context containing player information
     * @param enable True to enable, false to disable
     * @return Command result indicating success or failure
     */
    fun execute(
        ctx: CommandContext,
        enable: Boolean,
    ): CommandResult {
        val player = ctx.requirePlayer()
        val settings = settingsManager.getSettings(player.uniqueId)
        settingsManager.updateSettings(key.write(settings, enable))

        player.sendMessage(MessageFormatter.formatSuccess(message(key.toggleMessageKey, enable)))
        return CommandResult.Success
    }

    /**
     * Shows the current status of the setting for a player.
     *
     * @param ctx The command context containing player information
     * @return Command result indicating success or failure
     */
    fun showStatus(ctx: CommandContext): CommandResult {
        val player = ctx.requirePlayer()
        val settings = settingsManager.getSettings(player.uniqueId)

        player.sendMessage(MessageFormatter.format(message(key.statusMessageKey, key.read(settings))))
        return CommandResult.Success
    }

    private fun message(
        messageKey: String,
        enabled: Boolean,
    ): String = languageManager.getMessage(messageKey, mapOf("toggle" to languageManager.getToggleText(enabled)))
}
