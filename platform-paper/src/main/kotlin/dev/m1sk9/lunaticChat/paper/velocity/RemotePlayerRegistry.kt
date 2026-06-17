package dev.m1sk9.lunaticChat.paper.velocity

import dev.m1sk9.lunaticChat.engine.protocol.PresenceEntry
import java.util.concurrent.ConcurrentHashMap

/**
 * Caches proxy-wide player presence (player name -> server name) on this Paper server.
 *
 * Velocity is the source of truth; this registry is replaced wholesale on each
 * [dev.m1sk9.lunaticChat.engine.protocol.PluginMessage.PresenceSnapshot]. It backs
 * cross-server direct message tab-completion and remote target resolution.
 *
 * @param localServerName This server's name (as configured in velocityIntegration.serverName),
 *   used to exclude local players from remote lookups.
 */
class RemotePlayerRegistry(
    private val localServerName: String,
) {
    // lowercase player name -> presence entry (preserves original-cased name)
    private val players = ConcurrentHashMap<String, PresenceEntry>()

    /**
     * Replaces the entire roster with the given snapshot entries.
     */
    fun replaceAll(entries: List<PresenceEntry>) {
        players.clear()
        entries.forEach { players[it.playerName.lowercase()] = it }
    }

    /**
     * Clears the roster (e.g. when the Velocity connection is lost).
     */
    fun clear() {
        players.clear()
    }

    /**
     * Returns the server a player is currently on, or null if unknown.
     */
    fun serverOf(playerName: String): String? = players[playerName.lowercase()]?.serverName

    /**
     * Returns players on servers other than this one, with original-cased names.
     */
    fun remotePlayers(): List<PresenceEntry> =
        players.values
            .filter { !it.serverName.equals(localServerName, ignoreCase = true) }
            .toList()
}
