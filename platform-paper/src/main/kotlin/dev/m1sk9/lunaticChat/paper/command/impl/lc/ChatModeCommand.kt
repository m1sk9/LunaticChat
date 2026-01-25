package dev.m1sk9.lunaticChat.paper.command.impl.lc

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.chat.ChatModeManager
import dev.m1sk9.lunaticChat.paper.command.annotation.Permission
import dev.m1sk9.lunaticChat.paper.command.annotation.PlayerOnly
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.command.core.LunaticCommand
import dev.m1sk9.lunaticChat.paper.command.impl.lc.chatmode.ChatModeToggleCommand
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

@PlayerOnly
class ChatModeCommand(
    plugin: LunaticChat,
    private val chatModeManager: ChatModeManager,
    private val languageManager: LanguageManager,
) : LunaticCommand(plugin) {
    fun buildWithPermissionCheck(): LiteralArgumentBuilder<CommandSourceStack> {
        val builder = build()
        return applyMethodPermission("build", builder)
    }

    @Permission(LunaticChatPermissionNode.ChatMode::class)
    fun build(): LiteralArgumentBuilder<CommandSourceStack> {
        val chatModeCommand = Commands.literal("chatmode")

        // Add toggle subcommand
        chatModeCommand.then(
            ChatModeToggleCommand(
                plugin,
                chatModeManager,
                languageManager,
            ).buildWithPermissionCheck(),
        )

        // Default behavior: show current chat mode
        chatModeCommand.executes { ctx ->
            val context = wrapContext(ctx)
            checkPlayerOnly(context)?.let { return@executes handleResult(context, it) }

            val result = showCurrentMode(context)
            handleResult(context, result)
        }

        return chatModeCommand
    }

    private fun showCurrentMode(ctx: CommandContext): CommandResult {
        val sender = ctx.requirePlayer()
        val currentMode = chatModeManager.getChatMode(sender.uniqueId)

        val modeKey =
            when (currentMode) {
                dev.m1sk9.lunaticChat.engine.chat.ChatMode.GLOBAL -> "chatmode.mode.global"
                dev.m1sk9.lunaticChat.engine.chat.ChatMode.CHANNEL -> "chatmode.mode.channel"
            }

        val modeColor =
            when (currentMode) {
                dev.m1sk9.lunaticChat.engine.chat.ChatMode.GLOBAL -> NamedTextColor.GREEN
                dev.m1sk9.lunaticChat.engine.chat.ChatMode.CHANNEL -> NamedTextColor.AQUA
            }

        sender.sendMessage(
            Component
                .text(languageManager.getMessage("chatmode.current") + ": ", NamedTextColor.GRAY)
                .append(Component.text(languageManager.getMessage(modeKey), modeColor)),
        )

        return CommandResult.Success
    }

    override fun buildCommand(): LiteralArgumentBuilder<CommandSourceStack> =
        throw UnsupportedOperationException(
            "Should use build() method instead of buildCommand()",
        )
}
