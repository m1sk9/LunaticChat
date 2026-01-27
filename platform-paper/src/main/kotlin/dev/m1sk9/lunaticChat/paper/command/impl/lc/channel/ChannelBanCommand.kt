package dev.m1sk9.lunaticChat.paper.command.impl.lc.channel

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelRole
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.exception.ChannelNotFoundException
import dev.m1sk9.lunaticChat.engine.exception.ChannelPlayerAlreadyBannedException
import dev.m1sk9.lunaticChat.engine.exception.ChannelPlayerBypassBanException
import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelManager
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelMembershipManager
import dev.m1sk9.lunaticChat.paper.chat.handler.ChannelNotificationHandler
import dev.m1sk9.lunaticChat.paper.command.annotation.Permission
import dev.m1sk9.lunaticChat.paper.command.annotation.PlayerOnly
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.command.core.LunaticCommand
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.i18n.MessageFormatter
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.Bukkit

@PlayerOnly
class ChannelBanCommand(
    plugin: LunaticChat,
    private val channelManager: ChannelManager,
    private val membershipManager: ChannelMembershipManager,
    private val notificationHandler: ChannelNotificationHandler,
    private val languageManager: LanguageManager,
) : LunaticCommand(plugin) {
    fun buildWithPermissionCheck(): LiteralArgumentBuilder<CommandSourceStack> {
        val builder = build()
        return applyMethodPermission("build", builder)
    }

    @Permission(LunaticChatPermissionNode.ChannelBan::class)
    fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        Commands
            .literal("ban")
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

    private fun execute(
        ctx: CommandContext,
        playerName: String,
    ): CommandResult {
        val sender = ctx.requirePlayer()

        val channelId =
            channelManager.getPlayerChannel(sender.uniqueId)
                ?: return CommandResult.Failure(
                    MessageFormatter.formatError(
                        languageManager.getMessage("channel.ban.noActiveChannel"),
                    ),
                )

        // Check if sender has permission (OWNER or MODERATOR)
        val senderRole = membershipManager.getMemberRoleOrNull(sender.uniqueId, channelId)
        if (senderRole == null || senderRole == ChannelRole.MEMBER) {
            return CommandResult.Failure(
                MessageFormatter.formatError(
                    languageManager.getMessage("channel.ban.noPermission"),
                ),
            )
        }

        // Find target player
        val targetPlayer = Bukkit.getOfflinePlayer(playerName)

        // Check if player exists (has played before or is online)
        if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline) {
            return CommandResult.Failure(
                MessageFormatter.formatError(
                    languageManager.getMessage(
                        "channel.ban.playerNotFound",
                        mapOf("player" to playerName),
                    ),
                ),
            )
        }

        val targetPlayerId = targetPlayer.uniqueId

        // Check if banning self
        if (targetPlayerId == sender.uniqueId) {
            return CommandResult.Failure(
                MessageFormatter.formatError(
                    languageManager.getMessage("channel.ban.cannotBanSelf"),
                ),
            )
        }

        // Check if target has bypass permission
        val onlineTargetPlayer = Bukkit.getPlayer(playerName)
        if (onlineTargetPlayer != null && onlineTargetPlayer.hasPermission(LunaticChatPermissionNode.ChannelBypass.permissionNode)) {
            return CommandResult.Failure(
                MessageFormatter.formatError(
                    languageManager.getMessage(
                        "channel.ban.cannotBanBypass",
                        mapOf("player" to onlineTargetPlayer.name),
                    ),
                ),
            )
        }

        // Ban player from channel
        val banResult = channelManager.banPlayer(channelId, targetPlayerId)
        return banResult.fold(
            onSuccess = {
                val channel = channelManager.getChannel(channelId).getOrNull()
                val channelName = channel?.name ?: channelId

                // Send notification to banned player if online
                onlineTargetPlayer?.let { player ->
                    player.sendMessage(
                        MessageFormatter.format(
                            languageManager.getMessage(
                                "channel.ban.wasBanned",
                                mapOf("channel" to channelName, "banner" to sender.name),
                            ),
                        ),
                    )
                }

                // Broadcast ban notification to remaining members
                notificationHandler.broadcastBan(channelId, playerName, sender.name)

                CommandResult.SuccessWithMessage(
                    MessageFormatter.format(
                        languageManager.getMessage(
                            "channel.ban.success",
                            mapOf("player" to playerName, "channel" to channelName),
                        ),
                    ),
                )
            },
            onFailure = { error ->
                when (error) {
                    is ChannelNotFoundException -> {
                        CommandResult.Failure(
                            MessageFormatter.formatError(
                                languageManager.getMessage("channel.ban.error"),
                            ),
                        )
                    }
                    is ChannelPlayerBypassBanException -> {
                        CommandResult.Failure(
                            MessageFormatter.formatError(
                                languageManager.getMessage(
                                    "channel.ban.cannotBanBypass",
                                    mapOf("player" to playerName),
                                ),
                            ),
                        )
                    }
                    is ChannelPlayerAlreadyBannedException -> {
                        CommandResult.Failure(
                            MessageFormatter.formatError(
                                languageManager.getMessage(
                                    "channel.ban.alreadyBanned",
                                    mapOf("player" to playerName),
                                ),
                            ),
                        )
                    }
                    else -> {
                        CommandResult.Failure(
                            MessageFormatter.formatError(
                                languageManager.getMessage("channel.ban.error"),
                            ),
                        )
                    }
                }
            },
        )
    }

    override fun buildCommand(): LiteralArgumentBuilder<CommandSourceStack> =
        throw UnsupportedOperationException(
            "ChannelBanCommand should use build() method instead of buildCommand()",
        )
}
