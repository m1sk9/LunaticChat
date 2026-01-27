package dev.m1sk9.lunaticChat.paper.command.impl.lc.channel

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelRole
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.exception.ChannelNotFoundException
import dev.m1sk9.lunaticChat.engine.exception.ChannelPlayerNotBannedException
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
class ChannelUnbanCommand(
    plugin: LunaticChat,
    private val channelManager: ChannelManager,
    private val membershipManager: ChannelMembershipManager,
    private val languageManager: LanguageManager,
) : LunaticCommand(plugin) {
    fun buildWithPermissionCheck(): LiteralArgumentBuilder<CommandSourceStack> {
        val builder = build()
        return applyMethodPermission("build", builder)
    }

    @Permission(LunaticChatPermissionNode.ChannelUnban::class)
    fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        Commands
            .literal("unban")
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
                        languageManager.getMessage("channel.unban.noActiveChannel"),
                    ),
                )

        // Check if sender has permission (OWNER or MODERATOR)
        val senderRole = membershipManager.getMemberRoleOrNull(sender.uniqueId, channelId)
        if (senderRole == null || senderRole == ChannelRole.MEMBER) {
            return CommandResult.Failure(
                MessageFormatter.formatError(
                    languageManager.getMessage("channel.unban.noPermission"),
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
                        "channel.unban.playerNotFound",
                        mapOf("player" to playerName),
                    ),
                ),
            )
        }

        val targetPlayerId = targetPlayer.uniqueId

        // Unban player from channel
        val unbanResult = channelManager.unbanPlayer(channelId, targetPlayerId)
        return unbanResult.fold(
            onSuccess = {
                val channel = channelManager.getChannel(channelId).getOrNull()
                val channelName = channel?.name ?: channelId

                CommandResult.SuccessWithMessage(
                    MessageFormatter.format(
                        languageManager.getMessage(
                            "channel.unban.success",
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
                                languageManager.getMessage("channel.unban.error"),
                            ),
                        )
                    }
                    is ChannelPlayerNotBannedException -> {
                        CommandResult.Failure(
                            MessageFormatter.formatError(
                                languageManager.getMessage(
                                    "channel.unban.playerNotBanned",
                                    mapOf("player" to playerName),
                                ),
                            ),
                        )
                    }
                    else -> {
                        CommandResult.Failure(
                            MessageFormatter.formatError(
                                languageManager.getMessage("channel.unban.error"),
                            ),
                        )
                    }
                }
            },
        )
    }

    override fun buildCommand(): LiteralArgumentBuilder<CommandSourceStack> =
        throw UnsupportedOperationException(
            "ChannelUnbanCommand should use build() method instead of buildCommand()",
        )
}
