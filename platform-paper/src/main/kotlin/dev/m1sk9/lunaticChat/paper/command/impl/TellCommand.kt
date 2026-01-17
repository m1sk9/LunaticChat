package dev.m1sk9.lunaticChat.paper.command.impl

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.command.annotation.Command
import dev.m1sk9.lunaticChat.paper.command.annotation.Permission
import dev.m1sk9.lunaticChat.paper.command.annotation.PlayerOnly
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.command.core.LunaticCommand
import dev.m1sk9.lunaticChat.paper.command.handler.DirectMessageHandler
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import java.util.concurrent.CompletableFuture

@Command(
    name = "tell",
    aliases = ["t", "msg", "m", "w", "whisper"],
    description = "Send a private message to another player",
)
@Permission(LunaticChatPermissionNode.Tell::class)
@PlayerOnly
class TellCommand(
    plugin: LunaticChat,
    private val directMessageHandler: DirectMessageHandler,
) : LunaticCommand(plugin) {
    override fun buildCommand(): LiteralArgumentBuilder<CommandSourceStack> =
        Commands
            .literal(name)
            .then(
                Commands
                    .argument("player", StringArgumentType.word())
                    .suggests { _, builder -> suggestOnlinePlayers(builder) }
                    .then(
                        Commands
                            .argument("message", StringArgumentType.greedyString())
                            .executes { ctx ->
                                val context = wrapContext(ctx)
                                checkPlayerOnly(context)?.let { return@executes handleResult(context, it) }

                                val targetName = StringArgumentType.getString(ctx, "player")
                                val message = StringArgumentType.getString(ctx, "message")

                                val result = execute(context, targetName, message)
                                handleResult(context, result)
                            },
                    ),
            )

    private fun execute(
        ctx: CommandContext,
        targetName: String,
        message: String,
    ): CommandResult {
        val sender = ctx.requirePlayer()
        val recipient =
            Bukkit.getPlayer(targetName)
                ?: return CommandResult.Failure(
                    Component
                        .text("Player '$targetName' is not online.")
                        .color(NamedTextColor.RED),
                )

        if (recipient.uniqueId == sender.uniqueId) {
            return CommandResult.Failure(
                Component
                    .text("You cannot send a message to yourself.")
                    .color(NamedTextColor.RED),
            )
        }

        runBlocking {
            directMessageHandler.sendDirectMessage(sender, recipient, message)
        }

        return CommandResult.Success
    }

    private fun suggestOnlinePlayers(builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        val input = builder.remaining.lowercase()
        Bukkit
            .getOnlinePlayers()
            .filter { it.name.lowercase().startsWith(input) }
            .forEach { builder.suggest(it.name) }
        return builder.buildFuture()
    }
}
