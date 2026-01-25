package dev.m1sk9.lunaticChat.paper.command.impl.lc.channel

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelManager
import dev.m1sk9.lunaticChat.paper.command.annotation.Permission
import dev.m1sk9.lunaticChat.paper.command.annotation.PlayerOnly
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.command.core.LunaticCommand
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.i18n.MessageFormatter
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit

@PlayerOnly
class ChannelInfoCommand(
    plugin: LunaticChat,
    private val channelManager: ChannelManager,
    private val languageManager: LanguageManager,
) : LunaticCommand(plugin) {
    companion object {
        private const val MAX_MEMBERS_DISPLAY = 10
    }

    fun buildWithPermissionCheck(): LiteralArgumentBuilder<CommandSourceStack> {
        val builder = build()
        return applyMethodPermission("build", builder)
    }

    @Permission(LunaticChatPermissionNode.ChannelInfo::class)
    fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        Commands
            .literal("info")
            .executes { ctx ->
                val context = wrapContext(ctx)
                checkPlayerOnly(context)?.let { return@executes handleResult(context, it) }

                val result = execute(context, null)
                handleResult(context, result)
            }.then(
                Commands
                    .argument("channelId", StringArgumentType.word())
                    .suggests { _, builder ->
                        // Tab completion: suggest all public channel IDs
                        val channels = channelManager.getAllChannels().getOrNull() ?: emptyList()
                        channels.forEach { channel ->
                            builder.suggest(channel.id)
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

    private fun execute(
        ctx: CommandContext,
        channelIdArg: String?,
    ): CommandResult {
        val sender = ctx.requirePlayer()

        // Determine which channel to show info for
        val channelId =
            channelIdArg ?: channelManager.getPlayerChannel(sender.uniqueId)
                ?: return CommandResult.Failure(
                    MessageFormatter.formatError(
                        languageManager.getMessage("channel.info.noActiveChannel"),
                    ),
                )

        // Get channel
        val channel =
            channelManager.getChannel(channelId).getOrElse {
                return CommandResult.Failure(
                    MessageFormatter.formatError(
                        languageManager.getMessage(
                            "channel.info.notFound",
                            mapOf("channelId" to channelId),
                        ),
                    ),
                )
            }

        // Get members
        val members =
            channelManager.getChannelMembers(channelId).getOrElse {
                return CommandResult.Failure(
                    MessageFormatter.formatError(
                        languageManager.getMessage("channel.info.error"),
                    ),
                )
            }

        // Get owner name
        val ownerName = Bukkit.getOfflinePlayer(channel.ownerId).name ?: channel.ownerId.toString()

        // Display header
        sender.sendMessage(
            MessageFormatter.format(
                languageManager.getMessage("channel.info.header"),
            ),
        )

        // Display channel name
        sender.sendMessage(
            Component
                .text("  ")
                .append(Component.text(languageManager.getMessage("channel.info.name"), NamedTextColor.GRAY))
                .append(Component.text(": ", NamedTextColor.GRAY))
                .append(Component.text(channel.name, NamedTextColor.AQUA)),
        )

        // Display channel ID
        sender.sendMessage(
            Component
                .text("  ")
                .append(Component.text(languageManager.getMessage("channel.info.id"), NamedTextColor.GRAY))
                .append(Component.text(": ", NamedTextColor.GRAY))
                .append(Component.text(channel.id, NamedTextColor.YELLOW)),
        )

        // Display owner
        sender.sendMessage(
            Component
                .text("  ")
                .append(Component.text(languageManager.getMessage("channel.info.owner"), NamedTextColor.GRAY))
                .append(Component.text(": ", NamedTextColor.GRAY))
                .append(Component.text(ownerName, NamedTextColor.GOLD)),
        )

        // Display members
        val memberNames =
            members.mapNotNull { member ->
                Bukkit.getOfflinePlayer(member.playerId).name
            }

        val membersText =
            if (memberNames.size > MAX_MEMBERS_DISPLAY) {
                val displayNames = memberNames.take(MAX_MEMBERS_DISPLAY)
                val message =
                    languageManager.getMessage(
                        "channel.info.membersOmitted",
                        mapOf("count" to memberNames.size.toString()),
                    )
                Component
                    .text("  ")
                    .append(Component.text(languageManager.getMessage("channel.info.members"), NamedTextColor.GRAY))
                    .append(Component.text(": ", NamedTextColor.GRAY))
                    .append(Component.text(displayNames.joinToString(", "), NamedTextColor.WHITE))
                    .append(Component.text(" ... ", NamedTextColor.GRAY))
                    .append(Component.text("($message)", NamedTextColor.YELLOW))
            } else {
                Component
                    .text("  ")
                    .append(Component.text(languageManager.getMessage("channel.info.members"), NamedTextColor.GRAY))
                    .append(Component.text(": ", NamedTextColor.GRAY))
                    .append(Component.text(memberNames.joinToString(", "), NamedTextColor.WHITE))
            }

        sender.sendMessage(membersText)

        return CommandResult.Success
    }

    override fun buildCommand(): LiteralArgumentBuilder<CommandSourceStack> =
        throw UnsupportedOperationException(
            "ChannelInfoCommand should use build() method instead of buildCommand()",
        )
}
