package dev.m1sk9.lunaticChat.paper.command.impl.lc

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.debug.DebugState
import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.command.annotation.Command
import dev.m1sk9.lunaticChat.paper.command.annotation.Permission
import dev.m1sk9.lunaticChat.paper.command.annotation.PlayerOnly
import dev.m1sk9.lunaticChat.paper.command.core.LunaticCommand
import dev.m1sk9.lunaticChat.paper.command.setting.SettingHandlerRegistry
import dev.m1sk9.lunaticChat.paper.config.ConfigurationReloader
import dev.m1sk9.lunaticChat.paper.config.LunaticChatConfiguration
import dev.m1sk9.lunaticChat.paper.debug.DiagnosticsReport
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
    override val languageManager: LanguageManager,
    private val configuration: LunaticChatConfiguration,
    private val configurationReloader: ConfigurationReloader,
    private val debugState: DebugState,
    private val diagnosticsReport: DiagnosticsReport,
) : LunaticCommand(plugin) {
    override val description: String
        get() = languageManager.getMessage("commandDescription.lc")

    override fun buildCommand(): LiteralArgumentBuilder<CommandSourceStack> {
        val command = Commands.literal(name)

        SettingsCommand(
            plugin,
            settingHandlerRegistry,
            languageManager,
        ).buildAll().forEach { command.then(it) }

        StatusCommand(
            plugin,
            languageManager,
            configuration,
            debugState,
        ).buildAll().forEach { command.then(it) }

        ReloadCommand(
            plugin,
            languageManager,
            configurationReloader,
        ).buildAll().forEach { command.then(it) }

        DebugCommand(
            plugin,
            languageManager,
            debugState,
        ).buildAll().forEach { command.then(it) }

        DumpCommand(
            plugin,
            languageManager,
            diagnosticsReport,
        ).buildAll().forEach { command.then(it) }

        // Add channel command if channel manager is available
        plugin.channelManager?.let { manager ->
            plugin.channelMembershipManager?.let { membershipManager ->
                plugin.channelNotificationHandler?.let { notificationHandler ->
                    ChannelCommand(
                        plugin,
                        manager,
                        membershipManager,
                        notificationHandler,
                        languageManager,
                    ).buildAll().forEach { command.then(it) }
                }
            }
        }

        return command
    }
}
