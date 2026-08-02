package dev.m1sk9.lunaticChat.paper.command.impl.lc.channel

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelRole
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.exception.ChannelPlayerAlreadyBannedException
import dev.m1sk9.lunaticChat.engine.exception.ChannelPlayerBypassBanException
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
class ChannelBanCommand(
    plugin: LunaticChat,
    channelManager: ChannelManager,
    membershipManager: ChannelMembershipManager,
    private val notificationHandler: ChannelNotificationHandler,
    override val languageManager: LanguageManager,
) : ChannelSubCommand(plugin, channelManager, membershipManager) {
    override val literal = "ban"
    override val permissionNode = LunaticChatPermissionNode.ChannelBan

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
        if (target.uniqueId == sender.uniqueId) return failHere("cannotBanSelf")

        val targetName = target.name ?: playerName

        return membershipManager.banPlayer(target.uniqueId, channelId).fold(
            onSuccess = {
                val channelName = channelNameOf(channelId)

                Bukkit.getPlayer(playerName)?.sendMessage(
                    MessageFormatter.format(
                        languageManager.getMessage(
                            "channel.ban.wasBanned",
                            mapOf("channel" to channelName, "banner" to sender.name),
                        ),
                    ),
                )
                notificationHandler.broadcastBan(channelId, playerName, sender.name)

                okHere("success", mapOf("player" to playerName, "channel" to channelName))
            },
            onFailure = { error ->
                when (error) {
                    is ChannelPlayerBypassBanException -> failHere("cannotBanBypass", mapOf("player" to targetName))
                    is ChannelPlayerAlreadyBannedException -> failHere("alreadyBanned", mapOf("player" to playerName))
                    else -> failHere("error")
                }
            },
        )
    }
}
