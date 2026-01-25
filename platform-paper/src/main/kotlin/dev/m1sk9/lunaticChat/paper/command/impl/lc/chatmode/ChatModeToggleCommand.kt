package dev.m1sk9.lunaticChat.paper.command.impl.lc.chatmode

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.chat.ChatModeManager
import dev.m1sk9.lunaticChat.paper.command.annotation.Permission
import dev.m1sk9.lunaticChat.paper.command.annotation.PlayerOnly
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.command.core.LunaticCommand
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.i18n.MessageFormatter
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

@PlayerOnly
class ChatModeToggleCommand(
    plugin: LunaticChat,
    private val chatModeManager: ChatModeManager,
    private val languageManager: LanguageManager,
) : LunaticCommand(plugin) {
    fun buildWithPermissionCheck(): LiteralArgumentBuilder<CommandSourceStack> {
        val builder = build()
        return applyMethodPermission("build", builder)
    }

    @Permission(LunaticChatPermissionNode.ChatModeToggle::class)
    fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        Commands.literal("toggle").executes { ctx ->
            val context = wrapContext(ctx)
            checkPlayerOnly(context)?.let { return@executes handleResult(context, it) }

            val result = execute(context)
            handleResult(context, result)
        }

    private fun execute(ctx: CommandContext): CommandResult {
        val sender = ctx.requirePlayer()
        val newMode = chatModeManager.toggleChatMode(sender.uniqueId)

        val modeKey =
            when (newMode) {
                dev.m1sk9.lunaticChat.engine.chat.ChatMode.GLOBAL -> "chatmode.mode.global"
                dev.m1sk9.lunaticChat.engine.chat.ChatMode.CHANNEL -> "chatmode.mode.channel"
            }

        val modeColor =
            when (newMode) {
                dev.m1sk9.lunaticChat.engine.chat.ChatMode.GLOBAL -> NamedTextColor.GREEN
                dev.m1sk9.lunaticChat.engine.chat.ChatMode.CHANNEL -> NamedTextColor.AQUA
            }

        sender.sendMessage(
            Component
                .text()
                .append(MessageFormatter.formatSuccess(languageManager.getMessage("chatmode.toggle.success") + ": "))
                .append(Component.text(languageManager.getMessage(modeKey), modeColor))
                .build(),
        )

        return CommandResult.Success
    }

    override fun buildCommand(): LiteralArgumentBuilder<CommandSourceStack> =
        throw UnsupportedOperationException(
            "ChatModeToggleCommand should use build() method instead of buildCommand()",
        )
}
