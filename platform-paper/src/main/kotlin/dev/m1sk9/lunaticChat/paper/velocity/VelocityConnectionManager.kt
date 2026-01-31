package dev.m1sk9.lunaticChat.paper.velocity

import dev.m1sk9.lunaticChat.engine.protocol.PluginMessage
import dev.m1sk9.lunaticChat.engine.protocol.PluginMessageCodec
import dev.m1sk9.lunaticChat.engine.protocol.ProtocolVersion
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.messaging.PluginMessageListener
import java.util.concurrent.CompletableFuture
import java.util.logging.Logger

/**
 * Manages connection with Velocity proxy
 */
class VelocityConnectionManager(
    private val plugin: Plugin,
    private val pluginVersion: String,
    private val logger: Logger,
) : PluginMessageListener {
    companion object {
        private const val CHANNEL = "lunaticchat:main"
        private const val HANDSHAKE_TIMEOUT_SECONDS = 5L
    }

    /**
     * Connection state
     */
    enum class ConnectionState {
        DISCONNECTED,
        HANDSHAKING,
        CONNECTED,
        FAILED,
    }

    /**
     * Handshake result
     */
    sealed class HandshakeResult {
        data class Success(
            val velocityVersion: String,
        ) : HandshakeResult()

        data class Error(
            val message: String,
        ) : HandshakeResult()
    }

    private var state: ConnectionState = ConnectionState.DISCONNECTED
    private var handshakeFuture: CompletableFuture<HandshakeResult>? = null
    private var statusFuture: CompletableFuture<PluginMessage.StatusResponse>? = null
    private var velocityVersion: String? = null
    private var lastError: String? = null

    /**
     * Initialize
     */
    fun initialize() {
        plugin.server.messenger.registerOutgoingPluginChannel(plugin, CHANNEL)
        plugin.server.messenger.registerIncomingPluginChannel(plugin, CHANNEL, this)
        logger.info("Velocity integration channel registered: $CHANNEL")
    }

    /**
     * Performs handshake
     *
     * @param player Player to use for sending messages
     * @return Handshake result
     */
    fun performHandshake(player: Player): CompletableFuture<HandshakeResult> {
        if (state == ConnectionState.HANDSHAKING) {
            return handshakeFuture ?: CompletableFuture.completedFuture(
                HandshakeResult.Error("Handshake already in progress"),
            )
        }

        state = ConnectionState.HANDSHAKING
        lastError = null

        val future = CompletableFuture<HandshakeResult>()
        handshakeFuture = future

        val handshake =
            PluginMessage.Handshake(
                pluginVersion = pluginVersion,
                protocolMajor = ProtocolVersion.MAJOR,
                protocolMinor = ProtocolVersion.MINOR,
                protocolPatch = ProtocolVersion.PATCH,
            )

        val data = PluginMessageCodec.encode(handshake)
        player.sendPluginMessage(plugin, CHANNEL, data)

        logger.info("Sending handshake to Velocity (Plugin: $pluginVersion, Protocol: ${ProtocolVersion.version})")

        // Timeout handling
        plugin.server.scheduler.runTaskLater(
            plugin,
            Runnable {
                if (state == ConnectionState.HANDSHAKING) {
                    state = ConnectionState.FAILED
                    lastError = "Handshake timeout (${HANDSHAKE_TIMEOUT_SECONDS}s)"
                    logger.warning("Handshake timeout - Velocity plugin may not be installed")
                    future.complete(HandshakeResult.Error("Handshake timeout"))
                    handshakeFuture = null
                }
            },
            HANDSHAKE_TIMEOUT_SECONDS * 20L,
        )

        return future
    }

    /**
     * Sends status request
     *
     * @param player Player to use for sending messages
     * @return Status response
     */
    fun requestStatus(player: Player): CompletableFuture<PluginMessage.StatusResponse> {
        val future = CompletableFuture<PluginMessage.StatusResponse>()

        if (state != ConnectionState.CONNECTED) {
            future.completeExceptionally(IllegalStateException("Not connected to Velocity"))
            return future
        }

        statusFuture = future

        val statusRequest = PluginMessage.StatusRequest
        val data = PluginMessageCodec.encode(statusRequest)
        player.sendPluginMessage(plugin, CHANNEL, data)

        logger.info("Sending status request to Velocity")

        // Status request with timeout
        plugin.server.scheduler.runTaskLater(
            plugin,
            Runnable {
                if (!future.isDone) {
                    logger.warning("Status request timeout")
                    future.completeExceptionally(Exception("Status request timeout"))
                    statusFuture = null
                }
            },
            5L * 20L,
        )

        return future
    }

    /**
     * Plugin message received
     */
    override fun onPluginMessageReceived(
        channel: String,
        player: Player,
        message: ByteArray,
    ) {
        if (channel != CHANNEL) return

        try {
            val pluginMessage = PluginMessageCodec.decode(message)

            when (pluginMessage) {
                is PluginMessage.HandshakeResponse -> handleHandshakeResponse(pluginMessage)
                is PluginMessage.StatusResponse -> handleStatusResponse(pluginMessage)
                else -> logger.warning("Unexpected message type: ${pluginMessage::class.simpleName}")
            }
        } catch (e: Exception) {
            logger.severe("Failed to decode plugin message: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Handles handshake response
     */
    private fun handleHandshakeResponse(response: PluginMessage.HandshakeResponse) {
        val future = handshakeFuture ?: return

        if (response.compatible) {
            state = ConnectionState.CONNECTED
            velocityVersion = response.velocityVersion
            logger.info("Successfully connected to Velocity (version: ${response.velocityVersion})")
            future.complete(HandshakeResult.Success(response.velocityVersion))
        } else {
            state = ConnectionState.FAILED
            lastError = response.error ?: "Unknown compatibility error"
            logger.severe("Velocity handshake failed: ${response.error}")
            future.complete(HandshakeResult.Error(response.error ?: "Unknown error"))
        }

        handshakeFuture = null
    }

    /**
     * Handles status response
     */
    private fun handleStatusResponse(response: PluginMessage.StatusResponse) {
        logger.info("Received status response from Velocity: version=${response.velocityVersion}, protocol=${response.protocolVersion}, online=${response.online}")

        val future = statusFuture
        if (future != null) {
            future.complete(response)
            statusFuture = null
        } else {
            logger.warning("Received status response but no future was waiting for it")
        }
    }

    /**
     * Shutdown
     */
    fun shutdown() {
        plugin.server.messenger.unregisterOutgoingPluginChannel(plugin, CHANNEL)
        plugin.server.messenger.unregisterIncomingPluginChannel(plugin, CHANNEL)
        logger.info("Velocity integration channel unregistered")
    }

    /**
     * Gets current connection state
     */
    fun getState(): ConnectionState = state

    /**
     * Gets Velocity version (if connected)
     */
    fun getVelocityVersion(): String? = velocityVersion

    /**
     * Gets last error message
     */
    fun getLastError(): String? = lastError
}
