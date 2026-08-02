package dev.m1sk9.lunaticChat.paper.command.impl.lc

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelManager
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelMembershipManager
import dev.m1sk9.lunaticChat.paper.chat.handler.ChannelNotificationHandler
import dev.m1sk9.lunaticChat.paper.command.annotation.PlayerOnly
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.command.core.LunaticSubCommand
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
    override val languageManager: LanguageManager,
) : LunaticSubCommand(plugin) {
    override val literal = "channel"
    override val permissionNode = LunaticChatPermissionNode.Channel
    override val aliases = listOf("ch")

    override fun build(): LiteralArgumentBuilder<CommandSourceStack> {
        val channelCommand = Commands.literal(literal)

        subcommands().forEach { subcommand ->
            subcommand.buildAll().forEach { channelCommand.then(it) }
        }

        // Default help message when no subcommand is provided
        channelCommand.executes { ctx ->
            val context = wrapContext(ctx)
            checkPlayerOnly(context)?.let { return@executes handleResult(context, it) }

            val result = showHelp(context)
            handleResult(context, result)
        }

        return channelCommand
    }

    /**
     * The subcommands of /lc channel, in the order they are advertised by [showHelp].
     *
     * Registration and help share this one list so a new subcommand cannot appear in the tree
     * while staying invisible in the help output, or the reverse.
     */
    private fun subcommands(): List<LunaticSubCommand> =
        listOf(
            ChannelCreateCommand(plugin, channelManager, languageManager),
            ChannelListCommand(plugin, channelManager, languageManager),
            ChannelJoinCommand(plugin, channelManager, membershipManager, notificationHandler, languageManager),
            ChannelLeaveCommand(plugin, channelManager, membershipManager, notificationHandler, languageManager),
            ChannelSwitchCommand(plugin, channelManager, membershipManager, languageManager),
            ChannelStatusCommand(plugin, channelManager, membershipManager, languageManager),
            ChannelInfoCommand(plugin, channelManager, languageManager),
            ChannelDeleteCommand(plugin, channelManager, languageManager),
            ChannelInviteCommand(plugin, channelManager, membershipManager, languageManager),
            ChannelKickCommand(plugin, channelManager, membershipManager, notificationHandler, languageManager),
            ChannelBanCommand(plugin, channelManager, membershipManager, notificationHandler, languageManager),
            ChannelUnbanCommand(plugin, channelManager, membershipManager, languageManager),
            ChannelModCommand(plugin, channelManager, membershipManager, languageManager),
            ChannelOwnershipCommand(plugin, channelManager, membershipManager, languageManager),
        )

    private fun showHelp(ctx: CommandContext): CommandResult {
        val sender = ctx.requirePlayer()

        sender.sendMessage(
            MessageFormatter.format(
                languageManager.getMessage("channel.help.header"),
            ),
        )
        subcommands().forEach { subcommand ->
            sender.sendMessage(
                Component
                    .text("  ")
                    .append(
                        MessageFormatter.formatSuccess(
                            languageManager.getMessage("channel.help.${subcommand.literal}"),
                        ),
                    ),
            )
        }

        return CommandResult.Success
    }
}
