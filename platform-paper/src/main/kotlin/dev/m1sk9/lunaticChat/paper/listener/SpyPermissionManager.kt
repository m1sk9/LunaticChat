package dev.m1sk9.lunaticChat.paper.common

import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
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
     * Sends a copy of a message to every online spy that [exclude] does not reject.
     *
     * [message] is only invoked when someone will actually read the result, and the "you are
     * seeing this because you have spy permission" hover is built once for the whole audience
     * rather than per recipient. Spies are rare, so both matter on the message path.
     *
     * @param noticeText Text shown on hover, explaining why the reader is seeing the message
     * @param exclude Rejects players who are party to the message already
     * @param message Builds the message body
     */
    fun notifySpies(
        noticeText: String,
        exclude: (Player) -> Boolean,
        message: () -> Component,
    ) {
        val spies = directMessageSpyPlayers.values.filter { it.isOnline && !exclude(it) }
        if (spies.isEmpty()) return

        val withNotice = message().hoverEvent(HoverEvent.showText(Component.text(noticeText)))
        spies.forEach { it.sendMessage(withNotice) }
    }

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
