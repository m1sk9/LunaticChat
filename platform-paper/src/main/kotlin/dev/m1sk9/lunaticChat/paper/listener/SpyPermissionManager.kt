package dev.m1sk9.lunaticChat.paper.common

import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages spy permission cache for direct messages.
 * Caches player references to avoid repeated lookups.
 */
object SpyPermissionManager : Listener {
    private val directMessageSpyPlayers: ConcurrentHashMap<UUID, Player> = ConcurrentHashMap()

    /**
     * Gets all players with spy permission as a map of UUID to Player.
     */
    fun getDirectMessageSpyPlayers(): Map<UUID, Player> = directMessageSpyPlayers.toMap()

    /**
     * Gets all player UUIDs with spy permission.
     */
    fun getDirectMessageSpyPlayerIds(): Set<UUID> = directMessageSpyPlayers.keys

    /**
     * Updates the cache of players with direct message spy permission.
     * Call this on player join/quit/permission change events.
     */
    fun updateSpyCache() {
        directMessageSpyPlayers.clear()
        Bukkit
            .getOnlinePlayers()
            .filter {
                it.hasAllPermission {
                    +LunaticChatPermissionNode.Spy
                }
            }.associateByTo(directMessageSpyPlayers) { it.uniqueId }
    }

    @EventHandler(ignoreCancelled = true)
    fun onPlayerJoin(e: PlayerJoinEvent) {
        updateSpyCache()
    }

    @EventHandler(ignoreCancelled = true)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        directMessageSpyPlayers.remove(event.player.uniqueId)
    }
}
