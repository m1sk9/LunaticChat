package dev.m1sk9.lunaticChat.paper.listener

import dev.m1sk9.lunaticChat.paper.LunaticChat
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

class PlayerLogoutListener : Listener {
    @EventHandler
    fun onQuit(
        lunaticChat: LunaticChat,
        event: PlayerQuitEvent,
    ) {
        lunaticChat.directMessageHandler.clearPlayer(event.player)
    }
}
