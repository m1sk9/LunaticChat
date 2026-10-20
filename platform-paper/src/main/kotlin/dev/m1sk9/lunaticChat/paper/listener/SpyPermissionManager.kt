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
     * Sends a copy of a message to every online spy that [exclude] does not reject.
     *
     * [noticeText], [exclude] and [message] are only invoked when someone will actually read the
     * result, and the "you are seeing this because you have spy permission" hover is built once for
     * the whole audience rather than per recipient. Spies are rare, so all of that matters on the
     * message path - callers otherwise pay a translation lookup, and channel chat an O(members) set,
     * for an audience that is usually empty.
     *
     * @param noticeText Text shown on hover, explaining why the reader is seeing the message
     * @param exclude Rejects players who are party to the message already
     * @param message Builds the message body
     */
    fun notifySpies(
        noticeText: () -> String,
        exclude: (Player) -> Boolean,
        message: () -> Component,
    ) {
        if (directMessageSpyPlayers.isEmpty()) return

        // The permission is re-checked here because the cache is only refreshed per joining player:
        // without it, a spy whose permission is revoked while online keeps receiving copies until
        // they re-join.
        val spies = directMessageSpyPlayers.values.filter { it.isOnline && it.isSpy() && !exclude(it) }
        if (spies.isEmpty()) return

        val withNotice = message().hoverEvent(HoverEvent.showText(Component.text(noticeText())))
        spies.forEach { it.sendMessage(withNotice) }
    }

    /**
     * Rebuilds the cache from every online player.
     *
     * Called once when the listener is registered, so players already online when the plugin is
     * enabled are covered. Joins evaluate only the joining player instead of calling this, since a
     * full rescan per join is O(online players) permission lookups on the main thread.
     */
    fun updateSpyCache() {
        directMessageSpyPlayers.clear()
        Bukkit
            .getOnlinePlayers()
            .filter { it.isSpy() }
            .associateByTo(directMessageSpyPlayers) { it.uniqueId }
    }

    @EventHandler(ignoreCancelled = true)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (player.isSpy()) {
            directMessageSpyPlayers[player.uniqueId] = player
        } else {
            directMessageSpyPlayers.remove(player.uniqueId)
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        directMessageSpyPlayers.remove(event.player.uniqueId)
    }
}

private fun Player.isSpy(): Boolean = LunaticChatPermissionNode.Spy.has(this)
