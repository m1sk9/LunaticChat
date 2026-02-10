package dev.m1sk9.lunaticChat.paper.command.impl.lcv

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.engine.protocol.ProtocolVersion
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.command.annotation.Command
import dev.m1sk9.lunaticChat.paper.command.annotation.Permission
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.command.core.LunaticCommand
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.i18n.MessageFormatter
import dev.m1sk9.lunaticChat.paper.velocity.VelocityConnectionManager
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

/**
 * /lcv status command
 *
 * Displays Velocity integration status
 */
@Command(
    name = "lcv",
    aliases = ["lunaticvelocity"],
    description = "Velocity integration status",
)
@Permission(LunaticChatPermissionNode.VelocityStatus::class)
class VelocityStatusCommand(
    plugin: LunaticChat,
    private val velocityConnectionManager: VelocityConnectionManager,
    private val languageManager: LanguageManager,
) : LunaticCommand(plugin) {
    override val description: String
        get() = languageManager.getMessage("commandDescription.lcv")

    override fun buildCommand(): LiteralArgumentBuilder<CommandSourceStack> =
        Commands
            .literal(name)
            .then(
                Commands
                    .literal("status")
                    .executes { ctx ->
                        val context = wrapContext(ctx)
                        val result = execute(context)
                        handleResult(context, result)
                    },
            )

    private fun execute(ctx: CommandContext): CommandResult {
        val sender = ctx.sender
        val player = ctx.player
        val meta = plugin.pluginMeta

        // Connection state
        val state = velocityConnectionManager.getState()
        val stateMessage =
            when (state) {
                VelocityConnectionManager.ConnectionState.DISCONNECTED -> {
                    Component
                        .text(languageManager.getMessage("velocity.state.disconnected"), NamedTextColor.GRAY)
                        .hoverEvent(Component.text(languageManager.getMessage("velocity.hoverInfo.disconnected"), NamedTextColor.GRAY))
                }
                VelocityConnectionManager.ConnectionState.HANDSHAKING -> {
                    Component
                        .text(languageManager.getMessage("velocity.state.handshaking"), NamedTextColor.YELLOW)
                        .hoverEvent(Component.text(languageManager.getMessage("velocity.hoverInfo.handshaking"), NamedTextColor.YELLOW))
                }
                VelocityConnectionManager.ConnectionState.CONNECTED -> {
                    Component
                        .text(languageManager.getMessage("velocity.state.connected"), NamedTextColor.GREEN)
                        .hoverEvent(Component.text(languageManager.getMessage("velocity.hoverInfo.connected"), NamedTextColor.GREEN))
                }
                VelocityConnectionManager.ConnectionState.FAILED -> {
                    Component
                        .text(languageManager.getMessage("velocity.state.failed"), NamedTextColor.RED)
                        .hoverEvent(Component.text(languageManager.getMessage("velocity.hoverInfo.failed"), NamedTextColor.RED))
                }
            }

        val connectionStatus: List<Component?> =
            listOf(
                // Paper plugin version
                Component
                    .text("  • ", NamedTextColor.GRAY)
                    .append(Component.text("${languageManager.getMessage("velocity.status.label.paperVersion")}: ", NamedTextColor.GRAY))
                    .append(Component.text(meta.version, NamedTextColor.AQUA)),
                // Velocity version (if connected)
                velocityConnectionManager.getVelocityVersion()?.let { velocityVersion ->
                    Component
                        .text("  • ", NamedTextColor.GRAY)
                        .append(
                            Component.text("${languageManager.getMessage("velocity.status.label.velocityVersion")}: ", NamedTextColor.GRAY),
                        ).append(Component.text(velocityVersion, NamedTextColor.AQUA))
                },
                // Protocol version
                Component
                    .text("  • ", NamedTextColor.GRAY)
                    .append(Component.text("${languageManager.getMessage("velocity.status.label.protocolVersion")}: ", NamedTextColor.GRAY))
                    .append(Component.text(ProtocolVersion.version, NamedTextColor.AQUA)),
                // Connection state
                Component
                    .text("  • ", NamedTextColor.GRAY)
                    .append(Component.text("${languageManager.getMessage("velocity.status.label.connectionState")}: ", NamedTextColor.GRAY))
                    .append(stateMessage),
                // Error message (if failed)
                velocityConnectionManager.getLastError()?.let { error ->
                    Component
                        .text("  • ", NamedTextColor.GRAY)
                        .append(Component.text("${languageManager.getMessage("velocity.status.error")}: ", NamedTextColor.GRAY))
                        .append(Component.text(error, NamedTextColor.RED))
                },
            )

        sender.apply {
            // Header
            sendMessage(
                MessageFormatter.format(
                    languageManager.getMessage("velocity.status.header"),
                ),
            )
            // Connection status details
            connectionStatus.forEach { line ->
                line?.let { sendMessage(it) }
            }
        }

        // Live status check (only available for players when connected)
        if (state == VelocityConnectionManager.ConnectionState.CONNECTED) {
            if (player != null) {
                sender.sendMessage(
                    Component
                        .text("  • ", NamedTextColor.GRAY)
                        .append(Component.text(languageManager.getMessage("velocity.status.checkingLiveStatus"), NamedTextColor.YELLOW)),
                )

                velocityConnectionManager
                    .requestStatus(player)
                    .thenAccept { response ->
                        sender.sendMessage(
                            Component
                                .text("  ✓ ", NamedTextColor.GREEN)
                                .append(
                                    Component.text(
                                        "${languageManager.getMessage("velocity.status.label.onlinePlayers")}: ",
                                        NamedTextColor.GRAY,
                                    ),
                                ).append(Component.text(response.online.toString(), NamedTextColor.YELLOW)),
                        )
                    }.exceptionally { throwable ->
                        sender.sendMessage(
                            Component
                                .text("  ✗ ", NamedTextColor.RED)
                                .append(
                                    Component.text(
                                        "${languageManager.getMessage("velocity.status.liveStatusFailed")}: ",
                                        NamedTextColor.GRAY,
                                    ),
                                ).append(Component.text(throwable.message ?: "Unknown error", NamedTextColor.RED)),
                        )
                        null
                    }
            } else {
                // Console cannot perform live status check
                sender.sendMessage(
                    Component
                        .text("  • ", NamedTextColor.GRAY)
                        .append(Component.text(languageManager.getMessage("velocity.status.consoleNoLiveStatus"), NamedTextColor.GRAY)),
                )
            }
        }

        return CommandResult.Success
    }
}
