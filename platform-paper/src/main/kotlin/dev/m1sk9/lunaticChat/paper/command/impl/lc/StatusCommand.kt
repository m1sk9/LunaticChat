package dev.m1sk9.lunaticChat.paper.command.impl.lc

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.command.annotation.Permission
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.command.core.LunaticCommand
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.i18n.MessageFormatter
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent

class StatusCommand(
    plugin: LunaticChat,
    private val languageManager: LanguageManager,
) : LunaticCommand(plugin) {
    fun buildWithPermissionCheck(): LiteralArgumentBuilder<CommandSourceStack> {
        val builder = build()
        return applyMethodPermission("build", builder)
    }

    @Permission(LunaticChatPermissionNode.Status::class)
    fun build(): LiteralArgumentBuilder<CommandSourceStack> =
        Commands.literal("status").executes { ctx ->
            val context = wrapContext(ctx)
            checkPlayerOnly(context)?.let { return@executes handleResult(context, it) }

            val result = execute(context)
            handleResult(context, result)
        }

    private fun execute(ctx: CommandContext): CommandResult {
        val sender = ctx.requirePlayer()
        val meta = plugin.pluginMeta
        val urls =
            mapOf(
                "GitHub" to "https://github.com/m1sk9/LunaticChat",
                "Modrinth" to "https://modrinth.com/plugin/lunaticchat",
                "Website" to "https://lc.m1sk9.dev",
            )

        sender.apply {
            sendMessage(
                MessageFormatter
                    .format(
                        languageManager.getMessage("statusRunningVersion", mapOf("version" to meta.version)),
                    ).hoverEvent(HoverEvent.showText(Component.text(languageManager.getMessage("statusHover")))),
            )

            urls.forEach { (label, url) ->
                sendMessage(Component.text("§b$label: §r$url").clickEvent(ClickEvent.openUrl(url)))
            }
        }

        return CommandResult.Success
    }

    override fun buildCommand(): LiteralArgumentBuilder<CommandSourceStack> =
        throw UnsupportedOperationException(
            "Should use build() method instead of buildCommand()",
        )
}
