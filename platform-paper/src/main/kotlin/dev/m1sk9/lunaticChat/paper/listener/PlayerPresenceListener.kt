package dev.m1sk9.lunaticChat.paper.listener

import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.settings.PlayerSettingsManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerPresenceListener(
    private val lunaticChat: LunaticChat,
    private val settingsManager: PlayerSettingsManager?,
) : Listener {
    @EventHandler(ignoreCancelled = true)
    fun onJoin(event: PlayerJoinEvent) {
        settingsManager?.loadPlayerSettings(event.player.uniqueId)
    }

    @EventHandler(ignoreCancelled = true)
    fun onQuit(event: PlayerQuitEvent) {
        lunaticChat.directMessageHandler.clearPlayer(event.player)
        settingsManager?.unloadPlayerSettings(event.player.uniqueId)
    }
}
