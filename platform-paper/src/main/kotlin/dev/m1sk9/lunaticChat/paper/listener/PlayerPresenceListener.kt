package dev.m1sk9.lunaticChat.paper.listener

import dev.m1sk9.lunaticChat.paper.LunaticChat
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

class PlayerPresenceListener(
    private val lunaticChat: LunaticChat,
) : Listener {
    @EventHandler(ignoreCancelled = true)
    fun onQuit(event: PlayerQuitEvent) {
        lunaticChat.directMessageHandler.clearPlayer(event.player)
        // Settings remain in memory, no unloading needed
    }
}
