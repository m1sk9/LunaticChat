package dev.m1sk9.lunaticChat.paper.command.impl.lc.channel

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.exception.ChannelAlreadyActiveException
import dev.m1sk9.lunaticChat.engine.exception.ChannelMemberAlreadyException
import dev.m1sk9.lunaticChat.engine.exception.ChannelMemberLimitExceededException
import dev.m1sk9.lunaticChat.engine.exception.ChannelNotFoundException
import dev.m1sk9.lunaticChat.engine.exception.ChannelPlayerBannedException
import dev.m1sk9.lunaticChat.engine.exception.ChannelPlayerMembershipLimitExceededException
import dev.m1sk9.lunaticChat.engine.exception.ChannelPrivateRequiresInvitationException
import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelManager
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelMembershipManager
import dev.m1sk9.lunaticChat.paper.chat.handler.ChannelNotificationHandler
import dev.m1sk9.lunaticChat.paper.command.annotation.PlayerOnly
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.command.core.LunaticSubCommand
import dev.m1sk9.lunaticChat.paper.common.playChannelJoinNotification
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands

@PlayerOnly
class ChannelJoinCommand(
    plugin: LunaticChat,
    private val channelManager: ChannelManager,
    private val membershipManager: ChannelMembershipManager,
    private val notificationHandler: ChannelNotificationHandler,
    override val languageManager: LanguageManager,
) : LunaticSubCommand(plugin) {
    override val literal = "join"
    override val permissionNode = LunaticChatPermissionNode.ChannelJoin
    override val aliases = listOf("j")

    override fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        Commands
            .literal(literal)
            .then(
                Commands
                    .argument("channelId", StringArgumentType.word())
                    .suggests { _, builder ->
                        // Tab completion: suggest all public channel IDs
                        val channels = channelManager.getPublicChannels().getOrNull() ?: emptyList()
                        channels.forEach { channel ->
                            builder.suggest(channel.id)
                        }
                        builder.buildFuture()
                    }.executes { ctx ->
                        val context = wrapContext(ctx)
                        checkPlayerOnly(context)?.let { return@executes handleResult(context, it) }

                        val channelId = StringArgumentType.getString(ctx, "channelId")
                        val result = execute(context, channelId)
                        handleResult(context, result)
                    },
            )

    internal fun execute(
        ctx: CommandContext,
        channelId: String,
    ): CommandResult {
        val sender = ctx.requirePlayer()

        val result = membershipManager.joinChannel(sender.uniqueId, channelId)
        return result.fold(
            onSuccess = {
                val channel = channelManager.getChannel(channelId).getOrNull()

                // Play notification sound
                sender.playChannelJoinNotification()

                // Broadcast join notification to all channel members
                notificationHandler.broadcastJoin(channelId, sender.name)

                ok(
                    "channel.join.success",
                    mapOf("channelName" to (channel?.name ?: channelId), "channelId" to channelId),
                )
            },
            onFailure = { error ->
                when (error) {
                    is ChannelNotFoundException -> {
                        fail(
                            "channel.join.notFound",
                            mapOf("channelId" to channelId),
                        )
                    }
                    is ChannelAlreadyActiveException -> {
                        val channel = channelManager.getChannel(channelId).getOrNull()
                        fail(
                            "channel.join.alreadyActive",
                            mapOf("channelName" to (channel?.name ?: channelId)),
                        )
                    }
                    is ChannelMemberAlreadyException -> {
                        val channel = channelManager.getChannel(channelId).getOrNull()
                        fail(
                            "channel.join.alreadyMember",
                            mapOf("channelName" to (channel?.name ?: channelId)),
                        )
                    }
                    is ChannelMemberLimitExceededException -> {
                        fail(
                            "channel.join.channelMemberLimitExceeded",
                            mapOf("limit" to error.limit.toString()),
                        )
                    }
                    is ChannelPlayerMembershipLimitExceededException -> {
                        fail(
                            "channel.join.playerChannelLimitExceeded",
                            mapOf("limit" to error.limit.toString()),
                        )
                    }
                    is ChannelPlayerBannedException -> {
                        fail("channel.join.playerBanned")
                    }
                    is ChannelPrivateRequiresInvitationException -> {
                        fail("channel.join.privateChannel")
                    }
                    else -> {
                        fail("channel.join.error")
                    }
                }
            },
        )
    }
}
