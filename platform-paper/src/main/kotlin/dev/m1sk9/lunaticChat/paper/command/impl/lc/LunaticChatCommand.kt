package dev.m1sk9.lunaticChat.paper.command.impl.lc

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.command.annotation.Command
import dev.m1sk9.lunaticChat.paper.command.annotation.Permission
import dev.m1sk9.lunaticChat.paper.command.annotation.PlayerOnly
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
@Permission(LunaticChatPermissionNode.Lc::class)
@PlayerOnly
class LunaticChatCommand(
    plugin: LunaticChat,
    private val settingHandlerRegistry: SettingHandlerRegistry,
    private val languageManager: LanguageManager,
) : LunaticCommand(plugin) {
    override val description: String
        get() = languageManager.getMessage("commandDescription.lc")

    override fun buildCommand(): LiteralArgumentBuilder<CommandSourceStack> {
        val command =
            Commands
                .literal(name)
                .then(
                    SettingsCommand(
                        plugin,
                        settingHandlerRegistry,
                        languageManager,
                    ).buildWithPermissionCheck(),
                ).then(
                    StatusCommand(
                        plugin,
                        languageManager,
                    ).buildWithPermissionCheck(),
                )

        // Add channel command if channel manager is available
        plugin.channelManager?.let { manager ->
            plugin.channelMembershipManager?.let { membershipManager ->
                plugin.channelNotificationHandler?.let { notificationHandler ->
                    command.then(
                        ChannelCommand(
                            plugin,
                            manager,
                            membershipManager,
                            notificationHandler,
                            languageManager,
                        ).buildWithPermissionCheck(),
                    )
                }
            }
        }

        // Add chatmode command if chat mode manager is available
        plugin.chatModeManager?.let { chatModeManager ->
            command.then(
                ChatModeCommand(
                    plugin,
                    chatModeManager,
                    languageManager,
                ).buildWithPermissionCheck(),
            )
        }

        return command
    }
}
