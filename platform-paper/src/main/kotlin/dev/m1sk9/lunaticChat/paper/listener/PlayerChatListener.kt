package dev.m1sk9.lunaticChat.paper.listener

import dev.m1sk9.lunaticChat.engine.chat.ChatMode
import dev.m1sk9.lunaticChat.paper.chat.ChatModeManager
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelManager
import dev.m1sk9.lunaticChat.paper.chat.handler.ChannelMessageHandler
import dev.m1sk9.lunaticChat.paper.config.LunaticChatConfiguration
import dev.m1sk9.lunaticChat.paper.converter.RomanjiConverter
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.settings.PlayerSettingsManager
import dev.m1sk9.lunaticChat.paper.velocity.CrossServerChatManager
import io.papermc.paper.event.player.AsyncChatEvent
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

class PlayerChatListener(
    private val chatModeManager: ChatModeManager?,
    private val channelManager: ChannelManager?,
    private val channelMessageHandler: ChannelMessageHandler?,
    private val romajiConverter: RomanjiConverter?,
    private val settingsManager: PlayerSettingsManager,
    private val languageManager: LanguageManager,
    private val configuration: LunaticChatConfiguration,
    private val crossServerChatManager: CrossServerChatManager?,
) : Listener {
    private val plainTextSerializer = PlainTextComponentSerializer.plainText()

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onChat(event: AsyncChatEvent) {
        val player = event.player
        val settings = settingsManager.getSettings(player.uniqueId)

        val originalMessage = plainTextSerializer.serialize(event.message())

        // Handle chat mode switching with '!' prefix
        val hasPrefix = originalMessage.startsWith('!')
        val messageWithoutPrefix =
            if (hasPrefix) {
                originalMessage.removePrefix("!").trim()
            } else {
                originalMessage
            }

        if (hasPrefix && messageWithoutPrefix.isEmpty()) {
            event.isCancelled = true
            return
        }

        val effectiveMode =
            if (chatModeManager != null) {
                if (hasPrefix) {
                    val currentMode = chatModeManager.getChatMode(player.uniqueId)
                    currentMode.toggle()
                } else {
                    chatModeManager.getChatMode(player.uniqueId)
                }
            } else {
                // Default to GLOBAL when chatModeManager is not available
                ChatMode.GLOBAL
            }

        // Handle romaji conversion if enabled
        // Uses explicit timeout to prevent long blocking (1s max instead of 3s)
        // Note: AsyncChatEvent runs on async thread, so runBlocking here doesn't block main thread
        val displayMessage =
            if (settings.japaneseConversionEnabled && romajiConverter != null) {
                runCatching {
                    runBlocking {
                        kotlinx.coroutines
                            .withTimeoutOrNull(1000) {
                                romajiConverter
                                    ?.convert(messageWithoutPrefix)
                            }?.let { "$messageWithoutPrefix §e($it)" } ?: messageWithoutPrefix
                    }
                }.getOrElse { messageWithoutPrefix }
            } else {
                messageWithoutPrefix
            }

        // Route message based on chat mode
        when (effectiveMode) {
            ChatMode.GLOBAL -> {
                val velocityIntegrationEnabled = configuration.features.velocityIntegration.enabled
                val crossServerChatEnabled = configuration.features.velocityIntegration.crossServerGlobalChat

                Bukkit.getLogger().info(
                    "[LunaticChat DEBUG] Chat mode: GLOBAL, " +
                        "velocityEnabled=$velocityIntegrationEnabled, " +
                        "crossServerEnabled=$crossServerChatEnabled, " +
                        "managerNull=${crossServerChatManager == null}",
                )

                if (velocityIntegrationEnabled && crossServerChatEnabled && crossServerChatManager != null) {
                    // Send to Velocity for cross-server broadcast
                    crossServerChatManager.sendGlobalMessage(
                        player.uniqueId,
                        player.name,
                        displayMessage,
                    )

                    // Display as normal chat on the sender's server (no special formatting)
                    event.message(Component.text(displayMessage))
                } else {
                    // Existing behavior: normal Minecraft chat
                    event.message(Component.text(displayMessage))
                }
            }
            ChatMode.CHANNEL -> {
                // Channel chat requires channelManager and channelMessageHandler
                if (channelManager != null && channelMessageHandler != null) {
                    val hasActiveChannel = channelManager.getPlayerChannel(player.uniqueId) != null

                    if (hasActiveChannel) {
                        // Cancel event and clear all data to prevent other plugins from capturing it
                        // Even MONITOR priority listeners with ignoreCancelled=false won't get useful data
                        // Message will be delivered by ChannelMessageHandler instead
                        // Logging is handled by our own ChannelMessageLogger
                        event.isCancelled = true
                        event.viewers().clear()
                        event.message(Component.empty())
                        channelMessageHandler.sendChannelMessage(player, messageWithoutPrefix)
                    } else {
                        // Auto-fallback to global chat
                        event.message(Component.text(displayMessage))
                    }
                } else {
                    // Channel chat not available, fallback to normal chat
                    event.message(Component.text(displayMessage))
                }
            }
        }
    }
}
