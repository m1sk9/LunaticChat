package dev.m1sk9.lunaticChat.paper.command.impl.lcv

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.engine.protocol.ProtocolVersion
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.command.annotation.Command
import dev.m1sk9.lunaticChat.paper.command.annotation.Permission
import dev.m1sk9.lunaticChat.paper.command.annotation.PlayerOnly
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
@PlayerOnly
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
                        checkPlayerOnly(context)?.let { return@executes handleResult(context, it) }

                        val result = execute(context)
                        handleResult(context, result)
                    },
            )

    private fun execute(ctx: CommandContext): CommandResult {
        val sender = ctx.requirePlayer()
        val meta = plugin.pluginMeta

        // Connection state
        val state = velocityConnectionManager.getState()
        val stateMessage =
            when (state) {
                VelocityConnectionManager.ConnectionState.DISCONNECTED -> {
                    Component.text("Disconnected", NamedTextColor.GRAY)
                }
                VelocityConnectionManager.ConnectionState.HANDSHAKING -> {
                    Component.text("Handshaking...", NamedTextColor.YELLOW)
                }
                VelocityConnectionManager.ConnectionState.CONNECTED -> {
                    Component.text("Connected", NamedTextColor.GREEN)
                }
                VelocityConnectionManager.ConnectionState.FAILED -> {
                    Component.text("Failed", NamedTextColor.RED)
                }
            }

        // Header
        sender.sendMessage(
            MessageFormatter.format(
                languageManager.getMessage("velocity.status.header"),
            ),
        )

        // Paper plugin version
        sender.sendMessage(
            Component
                .text("  • ", NamedTextColor.GRAY)
                .append(Component.text("Paper Version: ", NamedTextColor.GRAY))
                .append(Component.text(meta.version, NamedTextColor.AQUA)),
        )

        // Protocol version
        sender.sendMessage(
            Component
                .text("  • ", NamedTextColor.GRAY)
                .append(Component.text("Protocol Version: ", NamedTextColor.GRAY))
                .append(Component.text(ProtocolVersion.version, NamedTextColor.AQUA)),
        )

        // Connection state
        sender.sendMessage(
            Component
                .text("  • ", NamedTextColor.GRAY)
                .append(Component.text("Connection State: ", NamedTextColor.GRAY))
                .append(stateMessage),
        )

        // Velocity version (if connected)
        velocityConnectionManager.getVelocityVersion()?.let { velocityVersion ->
            sender.sendMessage(
                Component
                    .text("  • ", NamedTextColor.GRAY)
                    .append(Component.text("Velocity Version: ", NamedTextColor.GRAY))
                    .append(Component.text(velocityVersion, NamedTextColor.AQUA)),
            )
        }

        // Error message (if failed)
        velocityConnectionManager.getLastError()?.let { error ->
            sender.sendMessage(
                Component
                    .text("  • ", NamedTextColor.GRAY)
                    .append(Component.text("Error: ", NamedTextColor.GRAY))
                    .append(Component.text(error, NamedTextColor.RED)),
            )
        }

        // Live status check (if connected)
        if (state == VelocityConnectionManager.ConnectionState.CONNECTED) {
            sender.sendMessage(
                Component
                    .text("  • ", NamedTextColor.GRAY)
                    .append(Component.text("Checking live status...", NamedTextColor.YELLOW)),
            )

            velocityConnectionManager
                .requestStatus(sender)
                .thenAccept { response ->
                    sender.sendMessage(
                        Component
                            .text("  ✓ ", NamedTextColor.GREEN)
                            .append(Component.text("Online Players: ", NamedTextColor.GRAY))
                            .append(Component.text(response.online.toString(), NamedTextColor.YELLOW)),
                    )
                }.exceptionally { throwable ->
                    sender.sendMessage(
                        Component
                            .text("  ✗ ", NamedTextColor.RED)
                            .append(Component.text("Live status check failed: ", NamedTextColor.GRAY))
                            .append(Component.text(throwable.message ?: "Unknown error", NamedTextColor.RED)),
                    )
                    null
                }
        }

        return CommandResult.Success
    }
}
