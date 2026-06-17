package dev.m1sk9.lunaticChat.paper.command.impl

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.chat.handler.DirectMessageHandler
import dev.m1sk9.lunaticChat.paper.command.annotation.Command
import dev.m1sk9.lunaticChat.paper.command.annotation.Permission
import dev.m1sk9.lunaticChat.paper.command.annotation.PlayerOnly
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.command.core.LunaticCommand
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.i18n.MessageFormatter
import dev.m1sk9.lunaticChat.paper.velocity.CrossServerDirectMessageManager
import dev.m1sk9.lunaticChat.paper.velocity.RemotePlayerRegistry
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture
import com.mojang.brigadier.context.CommandContext as BrigadierCommandContext

@Command(
    name = "tell",
    aliases = ["t", "msg", "m", "w", "whisper"],
    description = "",
)
@Permission(LunaticChatPermissionNode.Tell::class)
@PlayerOnly
class TellCommand(
    plugin: LunaticChat,
    private val directMessageHandler: DirectMessageHandler,
    private val languageManager: LanguageManager,
    private val crossServerDirectMessageManager: CrossServerDirectMessageManager? = null,
    private val remotePlayerRegistry: RemotePlayerRegistry? = null,
    private val localServerName: String = "",
) : LunaticCommand(plugin) {
    override val description: String
        get() = languageManager.getMessage("commandDescription.tell")

    private companion object {
        val WHITESPACE = Regex("\\s+")
    }

    override fun buildCommand(): LiteralArgumentBuilder<CommandSourceStack> =
        Commands
            .literal(name)
            .then(
                // A single greedy argument is used so the target token may contain '@'
                // (e.g. "<player>@<server>"), which Brigadier's word()/string() reject.
                Commands
                    .argument("input", StringArgumentType.greedyString())
                    .suggests { ctx, builder -> suggestTargets(ctx, builder) }
                    .executes { ctx ->
                        val context = wrapContext(ctx)
                        checkPlayerOnly(context)?.let { return@executes handleResult(context, it) }

                        val input = StringArgumentType.getString(ctx, "input")
                        val result = parseAndExecute(context, input)
                        handleResult(context, result)
                    },
            )

    internal fun parseAndExecute(
        ctx: CommandContext,
        input: String,
    ): CommandResult {
        val parts = input.trim().split(WHITESPACE, limit = 2)
        val targetName = parts[0]
        val message = parts.getOrNull(1)
        if (targetName.isEmpty() || message.isNullOrBlank()) {
            return CommandResult.Failure(
                MessageFormatter.formatError(
                    languageManager.getMessage("directMessage.usage"),
                ),
            )
        }
        return execute(ctx, targetName, message)
    }

    internal fun execute(
        ctx: CommandContext,
        targetName: String,
        message: String,
    ): CommandResult {
        val sender = ctx.requirePlayer()

        // Cross-server target: "<playerName>@<serverName>"
        if (targetName.contains('@')) {
            val manager =
                crossServerDirectMessageManager
                    ?: return CommandResult.Failure(
                        MessageFormatter.formatError(
                            languageManager.getMessage("directMessage.crossServerDisabled"),
                        ),
                    )
            val name = targetName.substringBefore('@')
            val server = targetName.substringAfter('@')
            if (name.isEmpty() || server.isEmpty()) {
                return CommandResult.Failure(
                    MessageFormatter.formatError(
                        languageManager.getMessage("directMessage.targetOffline", mapOf("target" to targetName)),
                    ),
                )
            }
            if (name.equals(sender.name, ignoreCase = true) &&
                server.equals(localServerName, ignoreCase = true)
            ) {
                return CommandResult.Failure(
                    MessageFormatter.formatError(
                        languageManager.getMessage("directMessage.yourself"),
                    ),
                )
            }
            manager.sendCrossServerMessage(sender, name, server, message)
            return CommandResult.Success
        }

        val recipient =
            Bukkit.getPlayerExact(targetName)
                ?: return CommandResult.Failure(
                    MessageFormatter.formatError(
                        languageManager.getMessage("directMessage.targetOffline", mapOf("target" to targetName)),
                    ),
                )

        if (recipient.uniqueId == sender.uniqueId) {
            return CommandResult.Failure(
                MessageFormatter.formatError(
                    languageManager.getMessage("directMessage.yourself"),
                ),
            )
        }

        directMessageHandler.sendDirectMessage(sender, recipient, message)

        return CommandResult.Success
    }

    private fun suggestTargets(
        ctx: BrigadierCommandContext<CommandSourceStack>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> {
        // Only complete the target token; once the message part begins, stop suggesting.
        val remaining = builder.remaining
        if (remaining.contains(' ')) {
            return builder.buildFuture()
        }
        val input = remaining.lowercase()
        val senderName = (ctx.source.sender as? Player)?.name

        // Local online players (same-server targets), excluding the sender themselves
        Bukkit
            .getOnlinePlayers()
            .filter { it.name != senderName && it.name.lowercase().startsWith(input) }
            .forEach { builder.suggest(it.name) }

        // Remote players (cross-server targets) as "<name>@<server>"
        val registry = remotePlayerRegistry
        if (crossServerDirectMessageManager != null && registry != null) {
            registry
                .remotePlayers()
                .map { "${it.playerName}@${it.serverName}" }
                .filter { it.lowercase().startsWith(input) }
                .forEach { builder.suggest(it) }
        }

        return builder.buildFuture()
    }
}
