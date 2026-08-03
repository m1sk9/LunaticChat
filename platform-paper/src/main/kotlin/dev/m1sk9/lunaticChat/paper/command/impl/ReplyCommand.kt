package dev.m1sk9.lunaticChat.paper.command.impl

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.PerPlayerWorkQueue
import dev.m1sk9.lunaticChat.paper.chat.handler.DirectMessageHandler
import dev.m1sk9.lunaticChat.paper.chat.handler.ReplyTarget
import dev.m1sk9.lunaticChat.paper.command.annotation.Command
import dev.m1sk9.lunaticChat.paper.command.annotation.Permission
import dev.m1sk9.lunaticChat.paper.command.annotation.PlayerOnly
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.command.core.LunaticCommand
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
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
    override val languageManager: LanguageManager,
    private val crossServerDirectMessageManager: CrossServerDirectMessageManager? = null,
    // Delivery is queued rather than run inline: romaji conversion can reach the Google IME API,
    // and a command executor runs on the tick thread. Queueing per sender keeps their messages in
    // the order they typed them.
    private val deliveryQueue: PerPlayerWorkQueue = plugin.deliveryQueue,
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
                ?: return fail("directMessage.replyTargetNotFound")

        return when (target) {
            is ReplyTarget.Local -> {
                val recipient =
                    Bukkit.getPlayer(target.uuid)
                        ?: return fail("directMessage.replyTargetNotFound")
                dmHandler.recordMessage(sender, recipient)
                deliveryQueue.submit(sender.uniqueId) { dmHandler.sendDirectMessage(sender, recipient, message) }
                CommandResult.Success
            }
            is ReplyTarget.Remote -> {
                val manager =
                    crossServerDirectMessageManager
                        ?: return fail("directMessage.replyTargetNotFound")
                dmHandler.recordRemoteRecipient(sender, target.playerName, target.serverName)
                deliveryQueue.submit(sender.uniqueId) {
                    manager.sendCrossServerMessage(sender, target.playerName, target.serverName, message)
                }
                CommandResult.Success
            }
        }
    }
}
