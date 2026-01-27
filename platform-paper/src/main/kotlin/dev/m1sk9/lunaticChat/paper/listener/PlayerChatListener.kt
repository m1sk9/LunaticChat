package dev.m1sk9.lunaticChat.paper.listener

import dev.m1sk9.lunaticChat.engine.chat.ChatMode
import dev.m1sk9.lunaticChat.paper.chat.ChatModeManager
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelManager
import dev.m1sk9.lunaticChat.paper.chat.handler.ChannelMessageHandler
import dev.m1sk9.lunaticChat.paper.converter.RomanjiConverter
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.i18n.MessageFormatter
import dev.m1sk9.lunaticChat.paper.settings.PlayerSettingsManager
import io.papermc.paper.event.player.AsyncChatEvent
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class PlayerChatListener(
    private val chatModeManager: ChatModeManager,
    private val channelManager: ChannelManager,
    private val channelMessageHandler: ChannelMessageHandler,
    private val romajiConverter: RomanjiConverter,
    private val settingsManager: PlayerSettingsManager,
    private val languageManager: LanguageManager,
) : Listener {
    private val plainTextSerializer = PlainTextComponentSerializer.plainText()

    @EventHandler(ignoreCancelled = true)
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
            if (hasPrefix) {
                val currentMode = chatModeManager.getChatMode(player.uniqueId)
                currentMode.toggle()
            } else {
                chatModeManager.getChatMode(player.uniqueId)
            }

        // Handle romaji conversion if enabled (requires blocking for HTTP call)
        val displayMessage =
            if (settings.japaneseConversionEnabled) {
                runCatching {
                    runBlocking {
                        romajiConverter
                            .convert(messageWithoutPrefix)
                            ?.let { "$messageWithoutPrefix §e($it)" }
                            ?: messageWithoutPrefix
                    }
                }.getOrNull() ?: messageWithoutPrefix
            } else {
                messageWithoutPrefix
            }

        // Route message based on chat mode
        when (effectiveMode) {
            ChatMode.GLOBAL -> {
                event.message(Component.text(displayMessage))
            }
            ChatMode.CHANNEL -> {
                val hasActiveChannel = channelManager.getPlayerChannel(player.uniqueId) != null

                if (hasActiveChannel) {
                    // Send to channel as normal
                    event.isCancelled = true
                    channelMessageHandler.sendChannelMessage(player, messageWithoutPrefix)
                } else {
                    // Auto-fallback to global chat
                    event.message(Component.text(displayMessage))

                    // Send warning to player
                    player.sendMessage(
                        MessageFormatter.format(
                            languageManager.getMessage("channel.autoFallback"),
                        ),
                    )
                }
            }
        }
    }
}
