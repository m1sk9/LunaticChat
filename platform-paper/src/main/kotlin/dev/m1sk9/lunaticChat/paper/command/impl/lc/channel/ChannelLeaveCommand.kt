package dev.m1sk9.lunaticChat.paper.command.impl.lc.channel

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.exception.ChannelNotMemberException
import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelManager
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelMembershipManager
import dev.m1sk9.lunaticChat.paper.chat.handler.ChannelNotificationHandler
import dev.m1sk9.lunaticChat.paper.command.annotation.PlayerOnly
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.command.core.LunaticSubCommand
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands

@PlayerOnly
class ChannelLeaveCommand(
    plugin: LunaticChat,
    private val channelManager: ChannelManager,
    private val membershipManager: ChannelMembershipManager,
    private val notificationHandler: ChannelNotificationHandler,
    override val languageManager: LanguageManager,
) : LunaticSubCommand(plugin) {
    override val literal = "leave"
    override val permissionNode = LunaticChatPermissionNode.ChannelLeave
    override val aliases = listOf("l")

    override fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        Commands.literal(literal).executes { ctx ->
            val context = wrapContext(ctx)
            checkPlayerOnly(context)?.let { return@executes handleResult(context, it) }

            val result = execute(context)
            handleResult(context, result)
        }

    internal fun execute(ctx: CommandContext): CommandResult {
        val sender = ctx.requirePlayer()

        // Get current channel before leaving
        val currentChannelId = channelManager.getPlayerChannel(sender.uniqueId)
        val currentChannel = currentChannelId?.let { channelManager.getChannel(it).getOrNull() }

        val result = membershipManager.leaveChannel(sender.uniqueId)
        return result.fold(
            onSuccess = {
                // Broadcast leave notification to all channel members
                if (currentChannelId != null) {
                    notificationHandler.broadcastLeave(currentChannelId, sender.name)
                }

                ok(
                    "channel.leave.success",
                    mapOf("channelName" to (currentChannel?.name ?: currentChannelId ?: "Unknown")),
                )
            },
            onFailure = { error ->
                when (error) {
                    is ChannelNotMemberException -> {
                        fail("channel.leave.noActiveChannel")
                    }
                    else -> {
                        fail("channel.leave.error")
                    }
                }
            },
        )
    }
}
