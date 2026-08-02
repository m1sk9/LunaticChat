package dev.m1sk9.lunaticChat.paper.command.impl.lc.channel

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.exception.ChannelNoOwnerPermissionException
import dev.m1sk9.lunaticChat.engine.exception.ChannelNotFoundException
import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelManager
import dev.m1sk9.lunaticChat.paper.command.annotation.PlayerOnly
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.command.core.LunaticSubCommand
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands

@PlayerOnly
class ChannelDeleteCommand(
    plugin: LunaticChat,
    private val channelManager: ChannelManager,
    override val languageManager: LanguageManager,
) : LunaticSubCommand(plugin) {
    override val literal = "delete"
    override val permissionNode = LunaticChatPermissionNode.ChannelDelete
    override val aliases = listOf("del")

    override fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        Commands
            .literal(literal)
            .then(
                Commands
                    .argument("channelId", StringArgumentType.word())
                    .suggests { ctx, builder ->
                        val player = ctx.source.executor as? org.bukkit.entity.Player
                        if (player != null) {
                            val hasBypass =
                                player.hasPermission(
                                    LunaticChatPermissionNode.ChannelBypass.permissionNode,
                                )

                            val channels =
                                if (hasBypass) {
                                    // Show all channels if has bypass permission
                                    channelManager.getAllChannels().getOrNull() ?: emptyList()
                                } else {
                                    // Show only owned channels
                                    channelManager
                                        .getAllChannels()
                                        .getOrNull()
                                        ?.filter { it.ownerId == player.uniqueId }
                                        ?: emptyList()
                                }

                            channels.forEach { channel ->
                                builder.suggest(channel.id)
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

    internal fun execute(
        ctx: CommandContext,
        channelId: String,
    ): CommandResult {
        val sender = ctx.requirePlayer()
        val hasBypass = sender.hasPermission(LunaticChatPermissionNode.ChannelBypass.permissionNode)

        val result = channelManager.deleteChannel(channelId, sender.uniqueId, hasBypass)
        return result.fold(
            onSuccess = {
                ok(
                    "channel.delete.success",
                    mapOf("id" to channelId),
                )
            },
            onFailure = { error ->
                when (error) {
                    is ChannelNotFoundException -> {
                        fail(
                            "channel.delete.notFound",
                            mapOf("id" to channelId),
                        )
                    }
                    is ChannelNoOwnerPermissionException -> {
                        fail("channel.delete.noPermission")
                    }
                    else -> {
                        fail("channel.delete.error")
                    }
                }
            },
        )
    }
}
