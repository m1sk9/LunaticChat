package dev.m1sk9.lunaticChat.paper.command.impl.lc.channel

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelRole
import dev.m1sk9.lunaticChat.engine.command.CommandResult
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
class ChannelOwnershipCommand(
    plugin: LunaticChat,
    channelManager: ChannelManager,
    membershipManager: ChannelMembershipManager,
    override val languageManager: LanguageManager,
) : ChannelSubCommand(plugin, channelManager, membershipManager) {
    override val literal = "ownership"
    override val permissionNode = LunaticChatPermissionNode.ChannelOwnership
    override val aliases = listOf("own")

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
        denyUnlessRole(sender.uniqueId, channelId, ChannelRole.OWNER)?.let { return it }

        val target = knownPlayer(playerName) ?: return failHere("playerNotFound", mapOf("player" to playerName))
        if (target.uniqueId == sender.uniqueId) return failHere("cannotTransferToSelf")

        val targetPlayerId = target.uniqueId
        if (membershipManager.getMemberRoleOrNull(targetPlayerId, channelId) == null) {
            return failHere("notMember", mapOf("player" to playerName))
        }

        // Transfer ownership
        val updateResult = channelManager.updateChannelOwner(channelId, targetPlayerId)
        return updateResult.fold(
            onSuccess = {
                val channelName = it.name

                // Notify target player
                val onlineTargetPlayer = Bukkit.getPlayer(playerName)
                onlineTargetPlayer?.let { player ->
                    player.sendMessage(
                        MessageFormatter.format(
                            languageManager.getMessage(
                                "channel.ownership.receivedOwnership",
                                mapOf("channel" to channelName, "previousOwner" to sender.name),
                            ),
                        ),
                    )
                }

                ok(
                    "channel.ownership.success",
                    mapOf("player" to playerName, "channel" to channelName),
                )
            },
            onFailure = { error ->
                fail("channel.ownership.error")
            },
        )
    }
}
