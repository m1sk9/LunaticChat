package dev.m1sk9.lunaticChat.paper.command.impl

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
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

@Command(
    name = "reply",
    aliases = ["r"],
    description = "Reply to the last person who messaged you",
)
@Permission(LunaticChatPermissionNode.Reply::class)
@PlayerOnly
class ReplyCommand(
    plugin: LunaticChat,
    private val dmHandler: DirectMessageHandler,
) : LunaticCommand(plugin) {
    override fun buildCommand(): LiteralArgumentBuilder<CommandSourceStack> =
        Commands
            .literal(name)
            .then(
                Commands
                    .argument("message", StringArgumentType.greedyString())
                    .executes { ctx ->
                        val context = wrapContext(ctx)

                        checkPlayerOnly(context)?.let { return@executes handleResult(context, it) }
                        val message = StringArgumentType.getString(ctx, "message")
                        val result = execute(context, message)

                        handleResult(context, result)
                    },
            )

    private fun execute(
        ctx: CommandContext,
        message: String,
    ): CommandResult {
        val sender = ctx.requirePlayer()
        val target =
            dmHandler.getReplyTarget(sender)
                ?: return CommandResult.Failure(
                    Component
                        .text("You have no one to reply to.")
                        .color(NamedTextColor.RED),
                )

        runBlocking {
            dmHandler.sendDirectMessage(sender, target, message)
        }

        return CommandResult.Success
    }
}
