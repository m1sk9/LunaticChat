package dev.m1sk9.lunaticChat.paper.command.impl

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.chat.handler.DirectMessageHandler
import dev.m1sk9.lunaticChat.paper.chat.handler.ReplyTarget
import dev.m1sk9.lunaticChat.paper.command.annotation.Command
import dev.m1sk9.lunaticChat.paper.command.annotation.Permission
import dev.m1sk9.lunaticChat.paper.command.annotation.PlayerOnly
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.command.core.LunaticCommand
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.i18n.MessageFormatter
import dev.m1sk9.lunaticChat.paper.velocity.CrossServerDirectMessageManager
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.Bukkit

@Command(
    name = "reply",
    aliases = ["r"],
    description = "",
)
@Permission(LunaticChatPermissionNode.Reply::class)
@PlayerOnly
class ReplyCommand(
    plugin: LunaticChat,
    private val dmHandler: DirectMessageHandler,
    private val languageManager: LanguageManager,
    private val crossServerDirectMessageManager: CrossServerDirectMessageManager? = null,
) : LunaticCommand(plugin) {
    override val description: String
        get() = languageManager.getMessage("commandDescription.reply")

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
                    MessageFormatter.formatError(
                        languageManager.getMessage("directMessage.replyTargetNotFound"),
                    ),
                )

        return when (target) {
            is ReplyTarget.Local -> {
                val recipient =
                    Bukkit.getPlayer(target.uuid)
                        ?: return CommandResult.Failure(
                            MessageFormatter.formatError(
                                languageManager.getMessage("directMessage.replyTargetNotFound"),
                            ),
                        )
                dmHandler.sendDirectMessage(sender, recipient, message)
                CommandResult.Success
            }
            is ReplyTarget.Remote -> {
                val manager =
                    crossServerDirectMessageManager
                        ?: return CommandResult.Failure(
                            MessageFormatter.formatError(
                                languageManager.getMessage("directMessage.replyTargetNotFound"),
                            ),
                        )
                manager.sendCrossServerMessage(sender, target.playerName, target.serverName, message)
                CommandResult.Success
            }
        }
    }
}
