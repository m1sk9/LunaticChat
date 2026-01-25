package dev.m1sk9.lunaticChat.paper.command.impl.lc.channel

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.exception.ChannelAlreadyActiveException
import dev.m1sk9.lunaticChat.engine.exception.ChannelMemberAlreadyException
import dev.m1sk9.lunaticChat.engine.exception.ChannelMemberLimitExceededException
import dev.m1sk9.lunaticChat.engine.exception.ChannelNotFoundException
import dev.m1sk9.lunaticChat.engine.exception.PlayerChannelLimitExceededException
import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelManager
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelMembershipManager
import dev.m1sk9.lunaticChat.paper.command.annotation.Permission
import dev.m1sk9.lunaticChat.paper.command.annotation.PlayerOnly
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.command.core.LunaticCommand
import dev.m1sk9.lunaticChat.paper.common.playChannelJoinNotification
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.i18n.MessageFormatter
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands

@PlayerOnly
class ChannelJoinCommand(
    plugin: LunaticChat,
    private val channelManager: ChannelManager,
    private val membershipManager: ChannelMembershipManager,
    private val languageManager: LanguageManager,
) : LunaticCommand(plugin) {
    fun buildWithPermissionCheck(): LiteralArgumentBuilder<CommandSourceStack> {
        val builder = build()
        return applyMethodPermission("build", builder)
    }

    @Permission(LunaticChatPermissionNode.ChannelJoin::class)
    fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        Commands
            .literal("join")
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

    private fun execute(
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

                CommandResult.SuccessWithMessage(
                    MessageFormatter.format(
                        languageManager.getMessage(
                            "channel.join.success",
                            mapOf("channelName" to (channel?.name ?: channelId), "channelId" to channelId),
                        ),
                    ),
                )
            },
            onFailure = { error ->
                when (error) {
                    is ChannelNotFoundException -> {
                        CommandResult.Failure(
                            MessageFormatter.formatError(
                                languageManager.getMessage(
                                    "channel.join.notFound",
                                    mapOf("channelId" to channelId),
                                ),
                            ),
                        )
                    }
                    is ChannelAlreadyActiveException -> {
                        val channel = channelManager.getChannel(channelId).getOrNull()
                        CommandResult.Failure(
                            MessageFormatter.formatError(
                                languageManager.getMessage(
                                    "channel.join.alreadyActive",
                                    mapOf("channelName" to (channel?.name ?: channelId)),
                                ),
                            ),
                        )
                    }
                    is ChannelMemberAlreadyException -> {
                        val channel = channelManager.getChannel(channelId).getOrNull()
                        CommandResult.Failure(
                            MessageFormatter.formatError(
                                languageManager.getMessage(
                                    "channel.join.alreadyMember",
                                    mapOf("channelName" to (channel?.name ?: channelId)),
                                ),
                            ),
                        )
                    }
                    is ChannelMemberLimitExceededException -> {
                        CommandResult.Failure(
                            MessageFormatter.formatError(
                                languageManager.getMessage(
                                    "channel.join.channelMemberLimitExceeded",
                                    mapOf("limit" to error.limit.toString()),
                                ),
                            ),
                        )
                    }
                    is PlayerChannelLimitExceededException -> {
                        CommandResult.Failure(
                            MessageFormatter.formatError(
                                languageManager.getMessage(
                                    "channel.join.playerChannelLimitExceeded",
                                    mapOf("limit" to error.limit.toString()),
                                ),
                            ),
                        )
                    }
                    else -> {
                        CommandResult.Failure(
                            MessageFormatter.formatError(
                                languageManager.getMessage("channel.join.error"),
                            ),
                        )
                    }
                }
            },
        )
    }

    override fun buildCommand(): LiteralArgumentBuilder<CommandSourceStack> =
        throw UnsupportedOperationException(
            "ChannelJoinCommand should use build() method instead of buildCommand()",
        )
}
