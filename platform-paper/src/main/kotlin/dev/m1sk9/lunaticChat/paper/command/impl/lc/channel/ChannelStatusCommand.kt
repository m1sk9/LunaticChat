package dev.m1sk9.lunaticChat.paper.command.impl.lc.channel

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelRole
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelManager
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelMembershipManager
import dev.m1sk9.lunaticChat.paper.command.annotation.PlayerOnly
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.command.core.LunaticSubCommand
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.i18n.MessageFormatter
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit

@PlayerOnly
class ChannelStatusCommand(
    plugin: LunaticChat,
    private val channelManager: ChannelManager,
    private val membershipManager: ChannelMembershipManager,
    override val languageManager: LanguageManager,
) : LunaticSubCommand(plugin) {
    override val literal = "status"
    override val permissionNode = LunaticChatPermissionNode.ChannelStatus
    override val aliases = listOf("st")

    override fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        Commands.literal(literal).executes { ctx ->
            val context = wrapContext(ctx)
            checkPlayerOnly(context)?.let { return@executes handleResult(context, it) }

            val result = execute(context)
            handleResult(context, result)
        }

    internal fun execute(ctx: CommandContext): CommandResult {
        val sender = ctx.requirePlayer()

        // Get active channel
        val activeChannelId = channelManager.getPlayerChannel(sender.uniqueId)
        val activeChannel = activeChannelId?.let { channelManager.getChannel(it).getOrNull() }

        // Get all player's channels
        val playerChannelIds = membershipManager.getPlayerChannels(sender.uniqueId)

        // Display header
        sender.sendMessage(
            MessageFormatter.format(
                languageManager.getMessage("channel.status.header"),
            ),
        )

        // Display active channel
        if (activeChannel != null) {
            val activeText =
                Component
                    .text("  ")
                    .append(
                        Component.text(
                            languageManager.getMessage("channel.status.activeChannel"),
                            NamedTextColor.GREEN,
                        ),
                    ).append(Component.text(": ", NamedTextColor.GRAY))
                    .append(
                        Component
                            .text(activeChannel.name, NamedTextColor.AQUA),
                    ).append(Component.text(" (", NamedTextColor.GRAY))
                    .append(Component.text(activeChannel.id, NamedTextColor.YELLOW))
                    .append(Component.text(")", NamedTextColor.GRAY))

            sender.sendMessage(activeText)

            // Display members of active channel
            val activeMembers =
                channelManager.getChannelMembers(activeChannelId).getOrElse {
                    emptyList()
                }

            if (activeMembers.isNotEmpty()) {
                val memberNames =
                    activeMembers.mapNotNull { member ->
                        val playerName = Bukkit.getOfflinePlayer(member.playerId).name ?: return@mapNotNull null
                        val roleText =
                            when (member.role) {
                                ChannelRole.OWNER -> " [OWNER]"
                                ChannelRole.MODERATOR -> " [MOD]"
                                ChannelRole.MEMBER -> ""
                            }
                        playerName + roleText
                    }

                val membersText = memberListLine(memberNames, indent = "    ", languageManager = languageManager)

                sender.sendMessage(membersText)
            }
        } else {
            sender.sendMessage(
                Component
                    .text("  ")
                    .append(
                        Component.text(
                            languageManager.getMessage("channel.status.noActiveChannel"),
                            NamedTextColor.GRAY,
                        ),
                    ),
            )
        }

        sender.sendMessage(Component.empty())

        // Display channels list
        if (playerChannelIds.isEmpty()) {
            sender.sendMessage(
                Component
                    .text("  ")
                    .append(
                        Component.text(
                            languageManager.getMessage("channel.status.noChannels"),
                            NamedTextColor.GRAY,
                        ),
                    ),
            )
        } else {
            sender.sendMessage(
                Component.text(
                    languageManager.getMessage(
                        "channel.status.channelList",
                        mapOf("count" to playerChannelIds.size.toString()),
                    ),
                    NamedTextColor.GOLD,
                ),
            )

            playerChannelIds.forEach { channelId ->
                val channel = channelManager.getChannel(channelId).getOrNull()
                if (channel != null) {
                    val isOwner = channel.ownerId == sender.uniqueId
                    val isActive = channelId == activeChannelId

                    val hoverTextBuilder =
                        Component
                            .text()
                            .append(Component.text("ID: ", NamedTextColor.GRAY))
                            .append(Component.text(channel.id, NamedTextColor.YELLOW))

                    if (isOwner) {
                        hoverTextBuilder
                            .append(Component.newline())
                            .append(Component.text("Role: ", NamedTextColor.GRAY))
                            .append(Component.text("Owner", NamedTextColor.GOLD))
                    }

                    if (!isActive) {
                        hoverTextBuilder
                            .append(Component.newline())
                            .append(Component.newline())
                            .append(Component.text(languageManager.getMessage("channel.status.clickToSwitch"), NamedTextColor.GREEN))
                            .append(Component.text(" ▶", NamedTextColor.YELLOW))
                    }

                    val hoverText = hoverTextBuilder.build()

                    var channelInfo =
                        Component
                            .text("  • ", NamedTextColor.GRAY)
                            .append(
                                Component
                                    .text(channel.name, if (isActive) NamedTextColor.GREEN else NamedTextColor.AQUA)
                                    .decorate(TextDecoration.ITALIC),
                            )

                    // Add owner indicator
                    if (isOwner) {
                        channelInfo = channelInfo.append(Component.text(" *", NamedTextColor.YELLOW))
                    }

                    channelInfo =
                        channelInfo
                            .append(Component.text(" (", NamedTextColor.GRAY))
                            .append(Component.text(channel.id, NamedTextColor.YELLOW))
                            .append(Component.text(")", NamedTextColor.GRAY))
                            .hoverEvent(HoverEvent.showText(hoverText))

                    // Add click event if not active
                    if (!isActive) {
                        channelInfo = channelInfo.clickEvent(ClickEvent.runCommand("/lc channel switch ${channel.id}"))
                    }

                    sender.sendMessage(channelInfo)
                }
            }
        }

        return CommandResult.Success
    }
}
