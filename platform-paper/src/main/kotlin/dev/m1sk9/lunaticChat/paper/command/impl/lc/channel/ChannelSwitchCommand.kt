package dev.m1sk9.lunaticChat.paper.command.impl.lc.channel

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.exception.ChannelAlreadyActiveException
import dev.m1sk9.lunaticChat.engine.exception.ChannelNotFoundException
import dev.m1sk9.lunaticChat.engine.exception.ChannelNotMemberException
import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelManager
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelMembershipManager
import dev.m1sk9.lunaticChat.paper.command.annotation.Permission
import dev.m1sk9.lunaticChat.paper.command.annotation.PlayerOnly
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.command.core.LunaticCommand
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.i18n.MessageFormatter
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands

@PlayerOnly
class ChannelSwitchCommand(
    plugin: LunaticChat,
    private val channelManager: ChannelManager,
    private val membershipManager: ChannelMembershipManager,
    private val languageManager: LanguageManager,
) : LunaticCommand(plugin) {
    fun buildWithPermissionCheck(): LiteralArgumentBuilder<CommandSourceStack> {
        val builder = build()
        return applyMethodPermission("build", builder)
    }

    @Permission(LunaticChatPermissionNode.ChannelSwitch::class)
    fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        Commands
            .literal("switch")
            .then(
                Commands
                    .argument("channelId", StringArgumentType.word())
                    .suggests { ctx, builder ->
                        // Tab completion: suggest channels the player is a member of
                        val sender = ctx.source.executor
                        if (sender is org.bukkit.entity.Player) {
                            val playerChannels = membershipManager.getPlayerChannels(sender.uniqueId).getOrNull() ?: emptyList()
                            playerChannels.forEach { channelId ->
                                builder.suggest(channelId)
                            }
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

        val result = membershipManager.switchChannel(sender.uniqueId, channelId)
        return result.fold(
            onSuccess = {
                val channel = channelManager.getChannel(channelId).getOrNull()

                CommandResult.SuccessWithMessage(
                    MessageFormatter.format(
                        languageManager.getMessage(
                            "channel.switch.success",
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
                                    "channel.switch.notFound",
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
                                    "channel.switch.alreadyActive",
                                    mapOf("channelName" to (channel?.name ?: channelId)),
                                ),
                            ),
                        )
                    }
                    is ChannelNotMemberException -> {
                        CommandResult.Failure(
                            MessageFormatter.formatError(
                                languageManager.getMessage(
                                    "channel.switch.notMember",
                                    mapOf("channelId" to channelId),
                                ),
                            ),
                        )
                    }
                    else -> {
                        CommandResult.Failure(
                            MessageFormatter.formatError(
                                languageManager.getMessage("channel.switch.error"),
                            ),
                        )
                    }
                }
            },
        )
    }

    override fun buildCommand(): LiteralArgumentBuilder<CommandSourceStack> =
        throw UnsupportedOperationException(
            "ChannelSwitchCommand should use build() method instead of buildCommand()",
        )
}
