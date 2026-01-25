package dev.m1sk9.lunaticChat.paper.command.impl.lc.channel

import com.mojang.brigadier.arguments.IntegerArgumentType
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
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import kotlin.math.ceil

@PlayerOnly
class ChannelListCommand(
    plugin: LunaticChat,
    private val channelManager: ChannelManager,
    private val languageManager: LanguageManager,
) : LunaticCommand(plugin) {
    companion object {
        private const val CHANNELS_PER_PAGE = 10
    }

    fun buildWithPermissionCheck(): LiteralArgumentBuilder<CommandSourceStack> {
        val builder = build()
        return applyMethodPermission("build", builder)
    }

    @Permission(LunaticChatPermissionNode.ChannelList::class)
    fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        Commands
            .literal("list")
            .executes { ctx ->
                val context = wrapContext(ctx)
                checkPlayerOnly(context)?.let { return@executes handleResult(context, it) }

                val result = execute(context, 1)
                handleResult(context, result)
            }.then(
                Commands
                    .argument("page", IntegerArgumentType.integer(1))
                    .executes { ctx ->
                        val context = wrapContext(ctx)
                        checkPlayerOnly(context)?.let { return@executes handleResult(context, it) }

                        val page = IntegerArgumentType.getInteger(ctx, "page")
                        val result = execute(context, page)
                        handleResult(context, result)
                    },
            )

    private fun execute(
        ctx: CommandContext,
        page: Int,
    ): CommandResult {
        val sender = ctx.requirePlayer()

        val result = channelManager.getPublicChannels()
        return result.fold(
            onSuccess = { channels ->
                if (channels.isEmpty()) {
                    sender.sendMessage(
                        MessageFormatter.format(
                            languageManager.getMessage("channel.list.empty"),
                        ),
                    )
                } else {
                    val totalPages = ceil(channels.size.toDouble() / CHANNELS_PER_PAGE).toInt()
                    val currentPage = page.coerceIn(1, totalPages)
                    val startIndex = (currentPage - 1) * CHANNELS_PER_PAGE
                    val endIndex = (startIndex + CHANNELS_PER_PAGE).coerceAtMost(channels.size)
                    val pageChannels = channels.subList(startIndex, endIndex)

                    sender.sendMessage(
                        MessageFormatter.format(
                            languageManager.getMessage(
                                "channel.list.header",
                                mapOf("count" to channels.size.toString()),
                            ),
                        ),
                    )

                    pageChannels.forEach { channel ->
                        val memberCountResult = channelManager.getChannelMembers(channel.id)
                        val memberCount =
                            memberCountResult.getOrNull()?.size
                                ?: 0

                        val hoverTextBuilder =
                            Component
                                .text()
                                .append(Component.text("ID: ", NamedTextColor.GRAY))
                                .append(Component.text(channel.id, NamedTextColor.YELLOW))
                                .append(Component.newline())
                                .append(Component.text("Members: ", NamedTextColor.GRAY))
                                .append(Component.text(memberCount.toString(), NamedTextColor.WHITE))

                        channel.description?.let { desc ->
                            hoverTextBuilder
                                .append(Component.newline())
                                .append(Component.text("Description: ", NamedTextColor.GRAY))
                                .append(Component.text(desc, NamedTextColor.WHITE))
                        }

                        hoverTextBuilder
                            .append(Component.newline())
                            .append(Component.newline())
                            .append(Component.text(languageManager.getMessage("channel.list.clickToJoin"), NamedTextColor.GREEN))
                            .append(Component.text(" ▶", NamedTextColor.YELLOW))

                        val hoverText = hoverTextBuilder.build()

                        val channelInfo =
                            Component
                                .text("  • ", NamedTextColor.GRAY)
                                .append(
                                    Component
                                        .text(channel.name, NamedTextColor.AQUA),
                                ).append(Component.text(" (", NamedTextColor.GRAY))
                                .append(Component.text(channel.id, NamedTextColor.YELLOW))
                                .append(Component.text(")", NamedTextColor.GRAY))
                                .hoverEvent(HoverEvent.showText(hoverText))
                                .clickEvent(ClickEvent.runCommand("/lc channel join ${channel.id}"))

                        sender.sendMessage(channelInfo)
                    }

                    // Display pagination footer
                    if (totalPages > 1) {
                        val paginationComponent =
                            Component
                                .text()
                                .append(Component.text("Page ", NamedTextColor.GRAY))
                                .append(Component.text("$currentPage", NamedTextColor.YELLOW))
                                .append(Component.text("/", NamedTextColor.GRAY))
                                .append(Component.text("$totalPages", NamedTextColor.YELLOW))

                        if (currentPage > 1) {
                            paginationComponent
                                .append(Component.text("  ", NamedTextColor.GRAY))
                                .append(
                                    Component
                                        .text("[◀]", NamedTextColor.GREEN)
                                        .clickEvent(ClickEvent.runCommand("/lc channel list ${currentPage - 1}"))
                                        .hoverEvent(
                                            HoverEvent.showText(
                                                Component.text(
                                                    "Go to page ${currentPage - 1}",
                                                    NamedTextColor.WHITE,
                                                ),
                                            ),
                                        ),
                                )
                        }

                        if (currentPage < totalPages) {
                            paginationComponent
                                .append(Component.text("  ", NamedTextColor.GRAY))
                                .append(
                                    Component
                                        .text("[▶]", NamedTextColor.GREEN)
                                        .clickEvent(ClickEvent.runCommand("/lc channel list ${currentPage + 1}"))
                                        .hoverEvent(
                                            HoverEvent.showText(
                                                Component.text(
                                                    "Go to page ${currentPage + 1}",
                                                    NamedTextColor.WHITE,
                                                ),
                                            ),
                                        ),
                                )
                        }

                        sender.sendMessage(paginationComponent.build())
                    }
                }
                CommandResult.Success
            },
            onFailure = { error ->
                CommandResult.Failure(
                    MessageFormatter.formatError(
                        languageManager.getMessage("channel.list.error"),
                    ),
                )
            },
        )
    }

    override fun buildCommand(): LiteralArgumentBuilder<CommandSourceStack> =
        throw UnsupportedOperationException(
            "ChannelListCommand should use build() method instead of buildCommand()",
        )
}
