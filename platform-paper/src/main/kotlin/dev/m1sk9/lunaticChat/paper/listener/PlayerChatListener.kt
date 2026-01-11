package dev.m1sk9.lunaticChat.paper.listener

import dev.m1sk9.lunaticChat.paper.converter.RomanjiConverter
import dev.m1sk9.lunaticChat.paper.settings.PlayerSettingsManager
import io.papermc.paper.event.player.AsyncChatEvent
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class PlayerChatListener(
    private val romajiConverter: RomanjiConverter,
    private val settingsManager: PlayerSettingsManager,
) : Listener {
    private val plainTextSerializer = PlainTextComponentSerializer.plainText()

    @EventHandler(ignoreCancelled = true)
    fun onChat(event: AsyncChatEvent) =
        runBlocking {
            val player = event.player
            val settings = settingsManager.getSettings(player.uniqueId)
            val message = event.message()
            val originalMessage = plainTextSerializer.serialize(message)

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

            event.message(Component.text(displayMessage))
        }
}
