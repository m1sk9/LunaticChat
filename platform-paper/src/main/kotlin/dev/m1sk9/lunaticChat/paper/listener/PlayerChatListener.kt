package dev.m1sk9.lunaticChat.paper.listener

import dev.m1sk9.lunaticChat.paper.converter.RomanjiConverter
import io.papermc.paper.event.player.AsyncChatEvent
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class PlayerChatListener(
    private val converter: RomanjiConverter,
) : Listener {
    private val plainTextSerializer = PlainTextComponentSerializer.plainText()

    @EventHandler(ignoreCancelled = true)
    fun onChat(event: AsyncChatEvent) {
        val message = event.message()
        val originalMessage = plainTextSerializer.serialize(message)

        // AsyncChatEvent is already running on an async thread, so runBlocking is safe
        val result = runBlocking {
            converter.convert(originalMessage)
        }

        event.message(Component.text("$originalMessage §e($result)"))
    }
}
