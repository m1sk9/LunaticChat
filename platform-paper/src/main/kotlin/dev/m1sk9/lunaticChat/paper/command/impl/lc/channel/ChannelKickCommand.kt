package dev.m1sk9.lunaticChat.paper.command.impl.lc.channel

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelRole
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.exception.ChannelPlayerBypassKickException
import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelManager
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelMembershipManager
import dev.m1sk9.lunaticChat.paper.chat.handler.ChannelNotificationHandler
import dev.m1sk9.lunaticChat.paper.command.annotation.PlayerOnly
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.i18n.MessageFormatter
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.Bukkit

@PlayerOnly
class ChannelKickCommand(
    plugin: LunaticChat,
    channelManager: ChannelManager,
    membershipManager: ChannelMembershipManager,
    private val notificationHandler: ChannelNotificationHandler,
    override val languageManager: LanguageManager,
) : ChannelSubCommand(plugin, channelManager, membershipManager) {
    override val literal = "kick"
    override val permissionNode = LunaticChatPermissionNode.ChannelKick
    override val aliases = listOf("k")

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
                                val members = channelManager.getChannelMembers(channelId).getOrNull() ?: emptyList()
                                members
                                    .filter { it.playerId != player.uniqueId }
                                    .forEach { member ->
                                        Bukkit.getOfflinePlayer(member.playerId).name?.let { name ->
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
        if (target.uniqueId == sender.uniqueId) return failHere("cannotKickSelf")

        val targetId = target.uniqueId
        val targetName = target.name ?: playerName

        if (!membershipManager.isMember(targetId, channelId).getOrElse { false }) {
            return failHere("notMember", mapOf("player" to playerName))
        }

        return membershipManager.kickPlayer(targetId, channelId).fold(
            onSuccess = {
                // Clear their active channel if this was it
                if (channelManager.getPlayerChannel(targetId) == channelId) {
                    channelManager.setPlayerChannel(targetId, null)
                }

                val channelName = channelNameOf(channelId)

                Bukkit.getPlayer(playerName)?.sendMessage(
                    MessageFormatter.format(
                        languageManager.getMessage(
                            "channel.kick.wasKicked",
                            mapOf("channel" to channelName, "kicker" to sender.name),
                        ),
                    ),
                )
                notificationHandler.broadcastKick(channelId, playerName, sender.name)

                okHere("success", mapOf("player" to playerName, "channel" to channelName))
            },
            onFailure = { error ->
                when (error) {
                    is ChannelPlayerBypassKickException -> failHere("cannotKickBypass", mapOf("player" to targetName))
                    else -> failHere("error")
                }
            },
        )
    }
}
