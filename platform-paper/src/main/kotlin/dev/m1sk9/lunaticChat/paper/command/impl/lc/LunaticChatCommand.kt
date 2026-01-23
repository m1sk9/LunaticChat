package dev.m1sk9.lunaticChat.paper.command.impl.lc

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.command.annotation.Command
import dev.m1sk9.lunaticChat.paper.command.core.LunaticCommand
import dev.m1sk9.lunaticChat.paper.command.setting.SettingHandlerRegistry
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands

/**
 * Main LunaticChat command (/lc).
 * This command serves as the entry point for all LunaticChat subcommands.
 */
@Command(
    name = "lc",
    aliases = ["lunaticchat"],
    description = "",
)
class LunaticChatCommand(
    plugin: LunaticChat,
    private val settingHandlerRegistry: SettingHandlerRegistry,
    private val languageManager: LanguageManager,
) : LunaticCommand(plugin) {
    override val description: String
        get() = languageManager.getMessage("commandDescription.lc")

    override fun buildCommand(): LiteralArgumentBuilder<CommandSourceStack> =
        Commands
            .literal(name)
            .then(
                SettingsCommand(
                    plugin,
                    settingHandlerRegistry,
                    languageManager,
                ).build(),
            )
}
