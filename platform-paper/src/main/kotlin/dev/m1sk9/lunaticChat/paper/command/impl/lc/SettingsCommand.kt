package dev.m1sk9.lunaticChat.paper.command.impl.lc

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.command.annotation.PlayerOnly
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.command.core.LunaticCommand
import dev.m1sk9.lunaticChat.paper.command.setting.SettingHandlerRegistry
import dev.m1sk9.lunaticChat.paper.command.setting.SettingKey
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.i18n.MessageFormatter
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands

/**
 * Setting subcommand for /lc command.
 * Handles /lc setting <key> [on|off]
 *
 * This subcommand is extensible - new settings can be added by:
 * 1. Adding a new SettingKey
 * 2. Creating a SettingHandler implementation
 * 3. Registering the handler in SettingHandlerRegistry
 */
@PlayerOnly
class SettingsCommand(
    plugin: LunaticChat,
    private val settingHandlerRegistry: SettingHandlerRegistry,
    private val languageManager: LanguageManager,
) : LunaticCommand(plugin) {
    /**
     * Builds the setting subcommand structure.
     * For each registered setting key, creates:
     * - /lc setting <key> on
     * - /lc setting <key> off
     * - /lc setting <key> (shows status)
     */
    fun build(): LiteralArgumentBuilder<CommandSourceStack> {
        val settingCommand = Commands.literal("settings")

        for (settingKey in SettingKey.Companion.values()) {
            val handler = settingHandlerRegistry.getHandler(settingKey)
            if (handler == null) {
                plugin.logger.warning("No handler registered for setting key: ${settingKey.key}")
                continue
            }

            val keyCommand =
                Commands
                    .literal(settingKey.key)
                    .then(
                        Commands
                            .literal("on")
                            .executes { ctx ->
                                val context = wrapContext(ctx)
                                checkPlayerOnly(context)?.let { return@executes handleResult(context, it) }
                                val result = handler.execute(context, true)
                                handleResult(context, result)
                            },
                    ).then(
                        Commands
                            .literal("off")
                            .executes { ctx ->
                                val context = wrapContext(ctx)
                                checkPlayerOnly(context)?.let { return@executes handleResult(context, it) }
                                val result = handler.execute(context, false)
                                handleResult(context, result)
                            },
                    ).executes { ctx ->
                        val context = wrapContext(ctx)
                        checkPlayerOnly(context)?.let { return@executes handleResult(context, it) }
                        val result = handler.showStatus(context)
                        handleResult(context, result)
                    }

            settingCommand.then(keyCommand)
        }

        settingCommand.executes { ctx ->
            val context = wrapContext(ctx)
            val result = showHelp(context)
            handleResult(context, result)
        }

        return settingCommand
    }

    /**
     * Shows help message for the setting subcommand.
     * Lists all available setting keys.
     */
    private fun showHelp(ctx: CommandContext): CommandResult {
        val availableKeys = settingHandlerRegistry.getAvailableKeys()
        val helpMessage =
            MessageFormatter.format(languageManager.getMessage("settingsAvailableValues", mapOf("values" to availableKeys.joinToString(", "))))

        ctx.reply(helpMessage)
        return CommandResult.Success
    }

    override fun buildCommand(): LiteralArgumentBuilder<CommandSourceStack> =
        throw UnsupportedOperationException(
            "SettingsSubcommand should use build() method instead of buildCommand()",
        )
}
