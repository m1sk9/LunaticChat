package dev.m1sk9.lunaticChat.paper.command.impl.lc

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelManager
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelMembershipManager
import dev.m1sk9.lunaticChat.paper.chat.handler.ChannelNotificationHandler
import dev.m1sk9.lunaticChat.paper.command.annotation.Permission
import dev.m1sk9.lunaticChat.paper.command.annotation.PlayerOnly
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.command.core.LunaticCommand
import dev.m1sk9.lunaticChat.paper.command.impl.lc.channel.ChannelBanCommand
import dev.m1sk9.lunaticChat.paper.command.impl.lc.channel.ChannelCreateCommand
import dev.m1sk9.lunaticChat.paper.command.impl.lc.channel.ChannelDeleteCommand
import dev.m1sk9.lunaticChat.paper.command.impl.lc.channel.ChannelInfoCommand
import dev.m1sk9.lunaticChat.paper.command.impl.lc.channel.ChannelInviteCommand
import dev.m1sk9.lunaticChat.paper.command.impl.lc.channel.ChannelJoinCommand
import dev.m1sk9.lunaticChat.paper.command.impl.lc.channel.ChannelKickCommand
import dev.m1sk9.lunaticChat.paper.command.impl.lc.channel.ChannelLeaveCommand
import dev.m1sk9.lunaticChat.paper.command.impl.lc.channel.ChannelListCommand
import dev.m1sk9.lunaticChat.paper.command.impl.lc.channel.ChannelModCommand
import dev.m1sk9.lunaticChat.paper.command.impl.lc.channel.ChannelOwnershipCommand
import dev.m1sk9.lunaticChat.paper.command.impl.lc.channel.ChannelStatusCommand
import dev.m1sk9.lunaticChat.paper.command.impl.lc.channel.ChannelSwitchCommand
import dev.m1sk9.lunaticChat.paper.command.impl.lc.channel.ChannelUnbanCommand
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.i18n.MessageFormatter
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component

@PlayerOnly
class ChannelCommand(
    plugin: LunaticChat,
    private val channelManager: ChannelManager,
    private val membershipManager: ChannelMembershipManager,
    private val notificationHandler: ChannelNotificationHandler,
    private val languageManager: LanguageManager,
) : LunaticCommand(plugin) {
    fun buildWithPermissionCheck(): LiteralArgumentBuilder<CommandSourceStack> {
        val builder = build()
        return applyMethodPermission("build", builder)
    }

    @Permission(LunaticChatPermissionNode.Channel::class)
    fun build(): LiteralArgumentBuilder<CommandSourceStack> {
        val channelCommand = Commands.literal("channel")

        // Add subcommands
        channelCommand
            .then(
                ChannelCreateCommand(
                    plugin,
                    channelManager,
                    languageManager,
                ).buildWithPermissionCheck(),
            ).then(
                ChannelListCommand(
                    plugin,
                    channelManager,
                    languageManager,
                ).buildWithPermissionCheck(),
            ).then(
                ChannelJoinCommand(
                    plugin,
                    channelManager,
                    membershipManager,
                    notificationHandler,
                    languageManager,
                ).buildWithPermissionCheck(),
            ).then(
                ChannelLeaveCommand(
                    plugin,
                    channelManager,
                    membershipManager,
                    notificationHandler,
                    languageManager,
                ).buildWithPermissionCheck(),
            ).then(
                ChannelSwitchCommand(
                    plugin,
                    channelManager,
                    membershipManager,
                    languageManager,
                ).buildWithPermissionCheck(),
            ).then(
                ChannelStatusCommand(
                    plugin,
                    channelManager,
                    membershipManager,
                    languageManager,
                ).buildWithPermissionCheck(),
            ).then(
                ChannelInfoCommand(
                    plugin,
                    channelManager,
                    languageManager,
                ).buildWithPermissionCheck(),
            ).then(
                ChannelDeleteCommand(
                    plugin,
                    channelManager,
                    languageManager,
                ).buildWithPermissionCheck(),
            ).then(
                ChannelInviteCommand(
                    plugin,
                    channelManager,
                    membershipManager,
                    languageManager,
                ).buildWithPermissionCheck(),
            ).then(
                ChannelKickCommand(
                    plugin,
                    channelManager,
                    membershipManager,
                    notificationHandler,
                    languageManager,
                ).buildWithPermissionCheck(),
            ).then(
                ChannelBanCommand(
                    plugin,
                    channelManager,
                    membershipManager,
                    notificationHandler,
                    languageManager,
                ).buildWithPermissionCheck(),
            ).then(
                ChannelUnbanCommand(
                    plugin,
                    channelManager,
                    membershipManager,
                    languageManager,
                ).buildWithPermissionCheck(),
            ).then(
                ChannelModCommand(
                    plugin,
                    channelManager,
                    membershipManager,
                    languageManager,
                ).buildWithPermissionCheck(),
            ).then(
                ChannelOwnershipCommand(
                    plugin,
                    channelManager,
                    membershipManager,
                    languageManager,
                ).buildWithPermissionCheck(),
            )

        // Default help message when no subcommand is provided
        channelCommand.executes { ctx ->
            val context = wrapContext(ctx)
            checkPlayerOnly(context)?.let { return@executes handleResult(context, it) }

            val result = showHelp(context)
            handleResult(context, result)
        }

        return channelCommand
    }

    private fun showHelp(ctx: CommandContext): CommandResult {
        val sender = ctx.requirePlayer()

        sender.sendMessage(
            MessageFormatter.format(
                languageManager.getMessage("channel.help.header"),
            ),
        )
        sender.sendMessage(
            Component
                .text("  ")
                .append(
                    MessageFormatter.formatSuccess(
                        languageManager.getMessage("channel.help.create"),
                    ),
                ),
        )
        sender.sendMessage(
            Component
                .text("  ")
                .append(
                    MessageFormatter.formatSuccess(
                        languageManager.getMessage("channel.help.list"),
                    ),
                ),
        )
        sender.sendMessage(
            Component
                .text("  ")
                .append(
                    MessageFormatter.formatSuccess(
                        languageManager.getMessage("channel.help.join"),
                    ),
                ),
        )
        sender.sendMessage(
            Component
                .text("  ")
                .append(
                    MessageFormatter.formatSuccess(
                        languageManager.getMessage("channel.help.leave"),
                    ),
                ),
        )
        sender.sendMessage(
            Component
                .text("  ")
                .append(
                    MessageFormatter.formatSuccess(
                        languageManager.getMessage("channel.help.switch"),
                    ),
                ),
        )
        sender.sendMessage(
            Component
                .text("  ")
                .append(
                    MessageFormatter.formatSuccess(
                        languageManager.getMessage("channel.help.status"),
                    ),
                ),
        )
        sender.sendMessage(
            Component
                .text("  ")
                .append(
                    MessageFormatter.formatSuccess(
                        languageManager.getMessage("channel.help.info"),
                    ),
                ),
        )
        sender.sendMessage(
            Component
                .text("  ")
                .append(
                    MessageFormatter.formatSuccess(
                        languageManager.getMessage("channel.help.delete"),
                    ),
                ),
        )
        sender.sendMessage(
            Component
                .text("  ")
                .append(
                    MessageFormatter.formatSuccess(
                        languageManager.getMessage("channel.help.invite"),
                    ),
                ),
        )
        sender.sendMessage(
            Component
                .text("  ")
                .append(
                    MessageFormatter.formatSuccess(
                        languageManager.getMessage("channel.help.kick"),
                    ),
                ),
        )
        sender.sendMessage(
            Component
                .text("  ")
                .append(
                    MessageFormatter.formatSuccess(
                        languageManager.getMessage("channel.help.ban"),
                    ),
                ),
        )
        sender.sendMessage(
            Component
                .text("  ")
                .append(
                    MessageFormatter.formatSuccess(
                        languageManager.getMessage("channel.help.unban"),
                    ),
                ),
        )
        sender.sendMessage(
            Component
                .text("  ")
                .append(
                    MessageFormatter.formatSuccess(
                        languageManager.getMessage("channel.help.mod"),
                    ),
                ),
        )
        sender.sendMessage(
            Component
                .text("  ")
                .append(
                    MessageFormatter.formatSuccess(
                        languageManager.getMessage("channel.help.ownership"),
                    ),
                ),
        )

        return CommandResult.Success
    }

    override fun buildCommand(): LiteralArgumentBuilder<CommandSourceStack> =
        throw UnsupportedOperationException(
            "Should use build() method instead of buildCommand()",
        )
}
