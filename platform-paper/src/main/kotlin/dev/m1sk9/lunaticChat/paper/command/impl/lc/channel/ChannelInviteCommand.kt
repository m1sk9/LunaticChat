package dev.m1sk9.lunaticChat.paper.command.impl.lc.channel

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelRole
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.exception.ChannelMemberLimitExceededException
import dev.m1sk9.lunaticChat.engine.exception.ChannelNotFoundException
import dev.m1sk9.lunaticChat.engine.exception.ChannelPlayerBannedException
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
class ChannelInviteCommand(
    plugin: LunaticChat,
    private val channelManager: ChannelManager,
    private val membershipManager: ChannelMembershipManager,
    private val languageManager: LanguageManager,
) : LunaticCommand(plugin) {
    fun buildWithPermissionCheck(): LiteralArgumentBuilder<CommandSourceStack> {
        val builder = build()
        return applyMethodPermission("build", builder)
    }

    @Permission(LunaticChatPermissionNode.ChannelInvite::class)
    fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        Commands
            .literal("invite")
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
                        languageManager.getMessage("channel.invite.noActiveChannel"),
                    ),
                )

        // Check if sender has permission (OWNER or MODERATOR)
        val senderRole = membershipManager.getMemberRoleOrNull(sender.uniqueId, channelId)
        if (senderRole == null || senderRole == ChannelRole.MEMBER) {
            return CommandResult.Failure(
                MessageFormatter.formatError(
                    languageManager.getMessage("channel.invite.noPermission"),
                ),
            )
        }

        // Find target player
        val targetPlayer =
            Bukkit.getPlayer(playerName)
                ?: return CommandResult.Failure(
                    MessageFormatter.formatError(
                        languageManager.getMessage(
                            "channel.invite.playerNotFound",
                            mapOf("player" to playerName),
                        ),
                    ),
                )

        // Check if inviting self
        if (targetPlayer.uniqueId == sender.uniqueId) {
            return CommandResult.Failure(
                MessageFormatter.formatError(
                    languageManager.getMessage("channel.invite.cannotInviteSelf"),
                ),
            )
        }

        // Check if player is banned
        val isBanned = channelManager.isPlayerBanned(channelId, targetPlayer.uniqueId).getOrElse { false }
        if (isBanned) {
            return CommandResult.Failure(
                MessageFormatter.formatError(
                    languageManager.getMessage(
                        "channel.invite.playerBanned",
                        mapOf("player" to targetPlayer.name),
                    ),
                ),
            )
        }

        // Attempt to join the target player to the channel (bypass private check for invites)
        val result = membershipManager.joinChannel(targetPlayer.uniqueId, channelId, bypassPrivateCheck = true)
        return result.fold(
            onSuccess = {
                // Send success message to sender
                val channel = channelManager.getChannel(channelId).getOrNull()
                val channelName = channel?.name ?: channelId

                // Send notification to invited player
                targetPlayer.sendMessage(
                    MessageFormatter.format(
                        languageManager.getMessage(
                            "channel.invite.receivedInvite",
                            mapOf("channel" to channelName, "inviter" to sender.name),
                        ),
                    ),
                )

                CommandResult.SuccessWithMessage(
                    MessageFormatter.format(
                        languageManager.getMessage(
                            "channel.invite.success",
                            mapOf("player" to targetPlayer.name, "channel" to channelName),
                        ),
                    ),
                )
            },
            onFailure = { error ->
                when (error) {
                    is ChannelNotFoundException -> {
                        CommandResult.Failure(
                            MessageFormatter.formatError(
                                languageManager.getMessage("channel.invite.error"),
                            ),
                        )
                    }
                    is ChannelMemberLimitExceededException -> {
                        CommandResult.Failure(
                            MessageFormatter.formatError(
                                languageManager.getMessage(
                                    "channel.invite.channelFull",
                                    mapOf("limit" to error.limit.toString()),
                                ),
                            ),
                        )
                    }
                    is ChannelPlayerBannedException -> {
                        CommandResult.Failure(
                            MessageFormatter.formatError(
                                languageManager.getMessage(
                                    "channel.invite.playerBanned",
                                    mapOf("player" to targetPlayer.name),
                                ),
                            ),
                        )
                    }
                    else -> {
                        CommandResult.Failure(
                            MessageFormatter.formatError(
                                languageManager.getMessage("channel.invite.error"),
                            ),
                        )
                    }
                }
            },
        )
    }

    override fun buildCommand(): LiteralArgumentBuilder<CommandSourceStack> =
        throw UnsupportedOperationException(
            "ChannelInviteCommand should use build() method instead of buildCommand()",
        )
}
