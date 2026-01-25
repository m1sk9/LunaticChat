package dev.m1sk9.lunaticChat.paper.listener

import dev.m1sk9.lunaticChat.engine.chat.ChatMode
import dev.m1sk9.lunaticChat.paper.chat.ChatModeManager
import dev.m1sk9.lunaticChat.paper.chat.handler.ChannelMessageHandler
import dev.m1sk9.lunaticChat.paper.converter.RomanjiConverter
import dev.m1sk9.lunaticChat.paper.settings.PlayerSettingsManager
import io.papermc.paper.event.player.AsyncChatEvent
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class PlayerChatListener(
    private val chatModeManager: ChatModeManager,
    private val channelMessageHandler: ChannelMessageHandler,
    private val romajiConverter: RomanjiConverter,
    private val settingsManager: PlayerSettingsManager,
) : Listener {
    private val plainTextSerializer = PlainTextComponentSerializer.plainText()

    @EventHandler(ignoreCancelled = true)
    fun onChat(event: AsyncChatEvent) =
        runBlocking {
            val player = event.player
            val settings = settingsManager.getSettings(player.uniqueId)

            val originalMessage = plainTextSerializer.serialize(event.message())
            val displayMessage =
                settings
                    .takeIf {
                        it.japaneseConversionEnabled
                    }?.runCatching {
                        romajiConverter
                            .convert(originalMessage)
                            ?.let { "$originalMessage §e($it)" }
                            ?: originalMessage
                    }?.getOrNull() ?: originalMessage

            val (_, effectiveMode) =
                if (originalMessage.startsWith('!')) {
                    val withoutPrefix = originalMessage.removePrefix("!").trim()
                    if (withoutPrefix.isEmpty()) {
                        event.isCancelled = true
                        return@runBlocking
                    }
                    val currentMode = chatModeManager.getChatMode(player.uniqueId)
                    withoutPrefix to currentMode.toggle()
                } else {
                    originalMessage to chatModeManager.getChatMode(player.uniqueId)
                }

            when (effectiveMode) {
                ChatMode.GLOBAL -> {
                    event.message(Component.text(displayMessage))
                }
                ChatMode.CHANNEL -> {
                    event.isCancelled = true
                    channelMessageHandler
                        .sendChannelMessage(player, originalMessage)
                }
            }

            event.message(Component.text(displayMessage))
        }
}
