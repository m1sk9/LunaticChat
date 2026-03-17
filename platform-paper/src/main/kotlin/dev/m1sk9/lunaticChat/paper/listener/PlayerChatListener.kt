package dev.m1sk9.lunaticChat.paper.listener

import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelManager
import dev.m1sk9.lunaticChat.paper.chat.handler.ChannelMessageHandler
import dev.m1sk9.lunaticChat.paper.config.LunaticChatConfiguration
import dev.m1sk9.lunaticChat.paper.converter.RomanjiConverter
import dev.m1sk9.lunaticChat.paper.converter.convertWithRomaji
import dev.m1sk9.lunaticChat.paper.settings.PlayerSettingsManager
import dev.m1sk9.lunaticChat.paper.velocity.CrossServerChatManager
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

class PlayerChatListener(
    private val channelManager: ChannelManager?,
    private val channelMessageHandler: ChannelMessageHandler?,
    private val romajiConverter: RomanjiConverter?,
    private val settingsManager: PlayerSettingsManager,
    private val configuration: LunaticChatConfiguration,
    private val crossServerChatManager: CrossServerChatManager?,
) : Listener {
    private val plainTextSerializer = PlainTextComponentSerializer.plainText()

    /**
     * Handles global chat message routing.
     * Checks if Velocity cross-server chat is enabled and routes accordingly.
     */
    private fun handleGlobalChat(
        event: AsyncChatEvent,
        displayMessage: String,
    ) {
        val velocityIntegrationEnabled = configuration.features.velocityIntegration.enabled
        val crossServerChatEnabled = configuration.features.velocityIntegration.crossServerGlobalChat

        if (velocityIntegrationEnabled && crossServerChatEnabled && crossServerChatManager != null) {
            // Send to Velocity for cross-server broadcast
            crossServerChatManager.sendGlobalMessage(
                event.player.uniqueId,
                event.player.name,
                displayMessage,
            )

            // Display as normal chat on the sender's server (no special formatting)
            event.message(Component.text(displayMessage))
        } else {
            // Existing behavior: normal Minecraft chat
            event.message(Component.text(displayMessage))
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onChat(event: AsyncChatEvent) {
        val player = event.player
        val settings = settingsManager.getSettings(player.uniqueId)

        val originalMessage = plainTextSerializer.serialize(event.message())

        // Handle '!' prefix for global chat override
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

        val displayMessage =
            if (settings.japaneseConversionEnabled && romajiConverter != null) {
                convertWithRomaji(messageWithoutPrefix, romajiConverter)
            } else {
                messageWithoutPrefix
            }

        // Determine if the player has an active channel
        val hasActiveChannel = channelManager?.getPlayerChannel(player.uniqueId) != null

        when {
            hasActiveChannel && !hasPrefix -> {
                // Channel chat: player is in a channel and no '!' prefix
                if (channelManager != null && channelMessageHandler != null) {
                    event.isCancelled = true
                    event.viewers().clear()
                    event.message(Component.empty())
                    channelMessageHandler.sendChannelMessage(player, displayMessage)
                } else {
                    handleGlobalChat(event, displayMessage)
                }
            }
            else -> {
                // Global chat: player has no active channel, or used '!' prefix to override
                handleGlobalChat(event, displayMessage)
            }
        }
    }
}
