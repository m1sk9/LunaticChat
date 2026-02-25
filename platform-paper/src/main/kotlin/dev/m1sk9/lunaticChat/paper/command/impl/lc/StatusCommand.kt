package dev.m1sk9.lunaticChat.paper.command.impl.lc

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.paper.BuildInfo
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.command.annotation.Permission
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.command.core.LunaticCommand
import dev.m1sk9.lunaticChat.paper.config.LunaticChatConfiguration
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.i18n.MessageFormatter
import dev.m1sk9.lunaticChat.paper.velocity.VelocityConnectionManager
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor

class StatusCommand(
    plugin: LunaticChat,
    private val languageManager: LanguageManager,
    private val configuration: LunaticChatConfiguration,
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

    internal fun execute(ctx: CommandContext): CommandResult {
        val sender = ctx.requirePlayer()
        val features = configuration.features

        // Version line with click-to-copy
        val versionText = BuildInfo.versionWithCommit()
        val versionLine =
            MessageFormatter
                .format(
                    languageManager.getMessage(
                        "status.runningVersion",
                        mapOf("version" to versionText),
                    ),
                ).hoverEvent(
                    HoverEvent.showText(
                        Component.text(languageManager.getMessage("status.clickToCopy"), NamedTextColor.GRAY),
                    ),
                ).clickEvent(ClickEvent.copyToClipboard(versionText))

        // Health status
        val velocityManager = plugin.velocityConnectionManager
        val isDegraded =
            features.velocityIntegration.enabled &&
                velocityManager != null &&
                velocityManager.getState() == VelocityConnectionManager.ConnectionState.FAILED
        val healthLine =
            if (isDegraded) {
                Component
                    .text("  • ", NamedTextColor.GRAY)
                    .append(Component.text("${languageManager.getMessage("status.label.health")}: ", NamedTextColor.GRAY))
                    .append(Component.text(languageManager.getMessage("status.health.degraded"), NamedTextColor.YELLOW))
                    .hoverEvent(
                        HoverEvent.showText(
                            Component.text(languageManager.getMessage("status.health.degradedHover"), NamedTextColor.YELLOW),
                        ),
                    )
            } else {
                Component
                    .text("  • ", NamedTextColor.GRAY)
                    .append(Component.text("${languageManager.getMessage("status.label.health")}: ", NamedTextColor.GRAY))
                    .append(Component.text(languageManager.getMessage("status.health.ok"), NamedTextColor.GREEN))
            }

        // Feature toggles
        val featureEntries =
            listOf(
                "status.feature.quickReplies" to features.quickReplies.enabled,
                "status.feature.japaneseConversion" to features.japaneseConversion.enabled,
                "status.feature.channelChat" to features.channelChat.enabled,
                "status.feature.velocityIntegration" to features.velocityIntegration.enabled,
            )

        // Configuration values
        val urls =
            mapOf(
                "GitHub" to "https://github.com/m1sk9/LunaticChat",
                "Modrinth" to "https://modrinth.com/plugin/lunaticchat",
                "Website" to "https://lc.m1sk9.dev",
            )

        sender.apply {
            sendMessage(versionLine)
            sendMessage(healthLine)

            // --- Features ---
            sendMessage(
                Component
                    .text("  ", NamedTextColor.GRAY)
                    .append(Component.text(languageManager.getMessage("status.label.features"), NamedTextColor.GRAY)),
            )
            featureEntries.forEach { (key, enabled) ->
                sendMessage(getToggleLine(languageManager.getMessage(key), enabled))
            }

            // --- Configuration ---
            sendMessage(
                Component
                    .text("  ", NamedTextColor.GRAY)
                    .append(Component.text(languageManager.getMessage("status.label.config"), NamedTextColor.GRAY)),
            )
            sendMessage(getToggleLine(languageManager.getMessage("status.config.debug"), configuration.debug))
            sendMessage(getToggleLine(languageManager.getMessage("status.config.checkForUpdates"), configuration.checkForUpdates))
            sendMessage(
                Component
                    .text("    • ", NamedTextColor.GRAY)
                    .append(Component.text("${languageManager.getMessage("status.config.language")}: ", NamedTextColor.GRAY))
                    .append(Component.text(configuration.language.code, NamedTextColor.AQUA)),
            )

            // Links
            urls.forEach { (label, url) ->
                sendMessage(
                    Component
                        .text("  • ", NamedTextColor.GRAY)
                        .append(Component.text("$label: ", NamedTextColor.AQUA))
                        .append(Component.text(url, NamedTextColor.WHITE))
                        .hoverEvent(
                            HoverEvent.showText(
                                Component.text(languageManager.getMessage("status.clickToOpen"), NamedTextColor.GRAY),
                            ),
                        ).clickEvent(ClickEvent.openUrl(url)),
                )
            }
        }

        return CommandResult.Success
    }

    private fun getToggleLine(
        label: String,
        enabled: Boolean,
    ): Component {
        val toggleText = languageManager.getMessage(if (enabled) "toggle.on" else "toggle.off")
        val color = if (enabled) NamedTextColor.GREEN else NamedTextColor.GRAY
        return Component
            .text("    • ", NamedTextColor.GRAY)
            .append(Component.text("$label: ", NamedTextColor.GRAY))
            .append(Component.text(toggleText, color))
    }

    override fun buildCommand(): LiteralArgumentBuilder<CommandSourceStack> =
        throw UnsupportedOperationException(
            "Should use build() method instead of buildCommand()",
        )
}
