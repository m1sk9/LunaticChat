package dev.m1sk9.lunaticChat.paper.command.impl.lc.channel

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelRole
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.exception.ChannelCannotInviteSelfException
import dev.m1sk9.lunaticChat.engine.exception.ChannelMemberLimitExceededException
import dev.m1sk9.lunaticChat.engine.exception.ChannelPlayerBannedException
import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelManager
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelMembershipManager
import dev.m1sk9.lunaticChat.paper.command.annotation.PlayerOnly
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.i18n.MessageFormatter
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.Bukkit

@PlayerOnly
class ChannelInviteCommand(
    plugin: LunaticChat,
    channelManager: ChannelManager,
    membershipManager: ChannelMembershipManager,
    override val languageManager: LanguageManager,
) : ChannelSubCommand(plugin, channelManager, membershipManager) {
    override val literal = "invite"
    override val permissionNode = LunaticChatPermissionNode.ChannelInvite
    override val aliases = listOf("inv")

    override fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        Commands
            .literal(literal)
            .then(
                Commands
                    .argument("playerName", StringArgumentType.word())
                    .suggests { _, builder ->
                        Bukkit
                            .getOnlinePlayers()
                            .filter { it.isOnline }
                            .forEach { player ->
                                builder.suggest(player.name)
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

        val targetPlayer =
            Bukkit.getPlayer(playerName)
                ?: return failHere("playerNotFound", mapOf("player" to playerName))

        val result = membershipManager.inviteToChannel(sender.uniqueId, targetPlayer.uniqueId, channelId)
        return result.fold(
            onSuccess = {
                val channelName = channelNameOf(channelId)

                // Send notification to invited player
                targetPlayer.sendMessage(
                    MessageFormatter.format(
                        languageManager.getMessage(
                            "channel.invite.receivedInvite",
                            mapOf("channel" to channelName, "inviter" to sender.name),
                        ),
                    ),
                )

                okHere("success", mapOf("player" to targetPlayer.name, "channel" to channelName))
            },
            onFailure = { error ->
                when (error) {
                    is ChannelCannotInviteSelfException -> failHere("cannotInviteSelf")
                    is ChannelMemberLimitExceededException -> failHere("channelFull", mapOf("limit" to error.limit.toString()))
                    is ChannelPlayerBannedException -> failHere("playerBanned", mapOf("player" to targetPlayer.name))
                    else -> failHere("error")
                }
            },
        )
    }
}
