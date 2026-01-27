package dev.m1sk9.lunaticChat.paper.command.impl.lc.channel

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelRole
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.exception.ChannelNotFoundException
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
import org.bukkit.Bukkit

@PlayerOnly
class ChannelModCommand(
    plugin: LunaticChat,
    private val channelManager: ChannelManager,
    private val membershipManager: ChannelMembershipManager,
    private val languageManager: LanguageManager,
) : LunaticCommand(plugin) {
    fun buildWithPermissionCheck(): LiteralArgumentBuilder<CommandSourceStack> {
        val builder = build()
        return applyMethodPermission("build", builder)
    }

    @Permission(LunaticChatPermissionNode.ChannelMod::class)
    fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        Commands
            .literal("mod")
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

        // Get sender's active channel
        val channelId =
            channelManager.getPlayerChannel(sender.uniqueId)
                ?: return CommandResult.Failure(
                    MessageFormatter.formatError(
                        languageManager.getMessage("channel.mod.noActiveChannel"),
                    ),
                )

        // Check if sender is OWNER
        val senderRole = membershipManager.getMemberRoleOrNull(sender.uniqueId, channelId)
        if (senderRole != ChannelRole.OWNER) {
            return CommandResult.Failure(
                MessageFormatter.formatError(
                    languageManager.getMessage("channel.mod.noPermission"),
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
                        "channel.mod.playerNotFound",
                        mapOf("player" to playerName),
                    ),
                ),
            )
        }

        val targetPlayerId = targetPlayer.uniqueId

        // Check if modding self
        if (targetPlayerId == sender.uniqueId) {
            return CommandResult.Failure(
                MessageFormatter.formatError(
                    languageManager.getMessage("channel.mod.cannotModSelf"),
                ),
            )
        }

        // Check if target is a member
        val targetRole = membershipManager.getMemberRoleOrNull(targetPlayerId, channelId)
        if (targetRole == null) {
            return CommandResult.Failure(
                MessageFormatter.formatError(
                    languageManager.getMessage(
                        "channel.mod.notMember",
                        mapOf("player" to playerName),
                    ),
                ),
            )
        }

        // Toggle mod status
        val newRole =
            if (targetRole == ChannelRole.MODERATOR) {
                ChannelRole.MEMBER
            } else {
                ChannelRole.MODERATOR
            }

        val updateResult = channelManager.updateMemberRole(channelId, targetPlayerId, newRole)
        return updateResult.fold(
            onSuccess = {
                val channel = channelManager.getChannel(channelId).getOrNull()
                val channelName = channel?.name ?: channelId

                val action =
                    if (newRole == ChannelRole.MODERATOR) {
                        languageManager.getMessage("channel.mod.promoted")
                    } else {
                        languageManager.getMessage("channel.mod.demoted")
                    }

                val onlineTargetPlayer = Bukkit.getPlayer(playerName)
                onlineTargetPlayer?.let { player ->
                    player.sendMessage(
                        MessageFormatter.format(
                            languageManager.getMessage(
                                "channel.mod.notification",
                                mapOf("action" to action, "channel" to channelName),
                            ),
                        ),
                    )
                }

                CommandResult.SuccessWithMessage(
                    MessageFormatter.format(
                        languageManager.getMessage(
                            "channel.mod.success",
                            mapOf("player" to playerName, "action" to action, "channel" to channelName),
                        ),
                    ),
                )
            },
            onFailure = { error ->
                when (error) {
                    is ChannelNotFoundException -> {
                        CommandResult.Failure(
                            MessageFormatter.formatError(
                                languageManager.getMessage("channel.mod.error"),
                            ),
                        )
                    }
                    else -> {
                        CommandResult.Failure(
                            MessageFormatter.formatError(
                                languageManager.getMessage("channel.mod.error"),
                            ),
                        )
                    }
                }
            },
        )
    }

    override fun buildCommand(): LiteralArgumentBuilder<CommandSourceStack> =
        throw UnsupportedOperationException(
            "ChannelModCommand should use build() method instead of buildCommand()",
        )
}
