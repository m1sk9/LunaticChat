package dev.m1sk9.lunaticChat.paper.command.impl.lc.channel

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelRole
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.exception.ChannelPlayerNotBannedException
import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelManager
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelMembershipManager
import dev.m1sk9.lunaticChat.paper.command.annotation.PlayerOnly
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.Bukkit

@PlayerOnly
class ChannelUnbanCommand(
    plugin: LunaticChat,
    channelManager: ChannelManager,
    membershipManager: ChannelMembershipManager,
    override val languageManager: LanguageManager,
) : ChannelSubCommand(plugin, channelManager, membershipManager) {
    override val literal = "unban"
    override val permissionNode = LunaticChatPermissionNode.ChannelUnban

    override fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        Commands
            .literal(literal)
            .then(
                Commands
                    .argument("playerName", StringArgumentType.word())
                    .suggests { ctx, builder ->
                        val player = ctx.source.executor as? org.bukkit.entity.Player
                        if (player != null) {
                            val channelId = channelManager.getPlayerChannel(player.uniqueId)
                            if (channelId != null) {
                                val channel = channelManager.getChannel(channelId).getOrNull()
                                channel?.bannedPlayers?.forEach { bannedId ->
                                    Bukkit.getOfflinePlayer(bannedId).name?.let { name ->
                                        builder.suggest(name)
                                    }
                                }
                            }
                        }
                        builder.buildFuture()
                    }.executes { ctx ->
                        val context = wrapContext(ctx)
                        checkPlayerOnly(context)?.let { return@executes handleResult(context, it) }

                        val playerName = StringArgumentType.getString(ctx, "playerName")
                        val result = execute(context, playerName)
                        handleResult(context, result)
                    },
            )

    internal fun execute(
        ctx: CommandContext,
        playerName: String,
    ): CommandResult {
        val sender = ctx.requirePlayer()

        val channelId = activeChannelOf(sender) ?: return failHere("noActiveChannel")
        denyUnlessRole(sender.uniqueId, channelId, ChannelRole.MODERATOR)?.let { return it }

        val target = knownPlayer(playerName) ?: return failHere("playerNotFound", mapOf("player" to playerName))

        return channelManager.unbanPlayer(channelId, target.uniqueId).fold(
            onSuccess = {
                okHere("success", mapOf("player" to playerName, "channel" to channelNameOf(channelId)))
            },
            onFailure = { error ->
                when (error) {
                    is ChannelPlayerNotBannedException -> failHere("playerNotBanned", mapOf("player" to playerName))
                    else -> failHere("error")
                }
            },
        )
    }
}
