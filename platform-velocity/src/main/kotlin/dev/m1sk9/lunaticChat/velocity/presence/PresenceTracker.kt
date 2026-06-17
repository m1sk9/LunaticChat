package dev.m1sk9.lunaticChat.velocity.presence

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.player.ServerPostConnectEvent
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier
import com.velocitypowered.api.proxy.server.RegisteredServer
import dev.m1sk9.lunaticChat.engine.protocol.PluginMessage
import dev.m1sk9.lunaticChat.engine.protocol.PluginMessageCodec
import dev.m1sk9.lunaticChat.engine.protocol.PresenceEntry
import org.slf4j.Logger

/**
 * Tracks proxy-wide player presence and pushes snapshots to Paper servers.
 *
 * Velocity is the source of truth for presence. Paper servers keep a cache that
 * is replaced wholesale on each [PluginMessage.PresenceSnapshot].
 */
class PresenceTracker(
    /**
     * Plugin instance for event registration. Type is [Any] because Velocity's
     * EventManager.register() accepts Object (mirrors [PluginMessageHandler]).
     */
    private val plugin: Any,
    private val server: ProxyServer,
    private val logger: Logger,
) {
    companion object {
        private val CHANNEL = MinecraftChannelIdentifier.create("lunaticchat", "main")
    }

    /**
     * Registers presence event listeners.
     */
    fun initialize() {
        server.eventManager.register(plugin, this)
        logger.info("Presence tracker registered")
    }

    /**
     * Broadcasts the current snapshot when a player joins or switches servers.
     */
    @Subscribe
    fun onServerPostConnect(event: ServerPostConnectEvent) {
        broadcastSnapshot()
    }

    /**
     * Broadcasts the current snapshot when a player disconnects, excluding them.
     */
    @Subscribe
    fun onDisconnect(event: DisconnectEvent) {
        broadcastSnapshot(exclude = event.player)
    }

    /**
     * Unregisters presence event listeners.
     */
    fun shutdown() {
        server.eventManager.unregisterListener(plugin, this)
        logger.info("Presence tracker unregistered")
    }

    /**
     * Sends the current snapshot to a single server (initial sync on request).
     */
    fun sendSnapshotTo(target: RegisteredServer) {
        val data = PluginMessageCodec.encode(buildSnapshot())
        target.sendPluginMessage(CHANNEL, data)
    }

    private fun broadcastSnapshot(exclude: Player? = null) {
        try {
            val data = PluginMessageCodec.encode(buildSnapshot(exclude))
            server.allServers.forEach { it.sendPluginMessage(CHANNEL, data) }
        } catch (e: Exception) {
            logger.error("Failed to broadcast presence snapshot: ${e.message}", e)
        }
    }

    private fun buildSnapshot(exclude: Player? = null): PluginMessage.PresenceSnapshot {
        val entries =
            server.allPlayers
                .asSequence()
                .filter { exclude == null || it.uniqueId != exclude.uniqueId }
                .mapNotNull { player ->
                    val serverName =
                        player.currentServer
                            .orElse(null)
                            ?.serverInfo
                            ?.name ?: return@mapNotNull null
                    PresenceEntry(playerName = player.username, serverName = serverName)
                }.toList()
        return PluginMessage.PresenceSnapshot(players = entries)
    }
}
