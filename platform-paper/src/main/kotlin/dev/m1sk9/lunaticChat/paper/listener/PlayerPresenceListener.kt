package dev.m1sk9.lunaticChat.paper.listener

import dev.m1sk9.lunaticChat.engine.chat.ChatMode
import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.chat.ChatModeManager
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelManager
import dev.m1sk9.lunaticChat.paper.common.hasAnyPermission
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.i18n.MessageFormatter
import dev.m1sk9.lunaticChat.paper.settings.PlayerSettingsManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.concurrent.atomic.AtomicBoolean

class PlayerPresenceListener(
    private val lunaticChat: LunaticChat,
    private val languageManager: LanguageManager,
    private val updateCheckerFlag: AtomicBoolean,
    private val playerSettingsManager: PlayerSettingsManager,
    private val chatModeManager: ChatModeManager? = null,
    private val channelManager: ChannelManager? = null,
) : Listener {
    @EventHandler(ignoreCancelled = true)
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player

        // Send update notification if available
        if (updateCheckerFlag.get() && player.hasAnyPermission { +LunaticChatPermissionNode.NoticeUpdate }) {
            player.sendMessage {
                Component
                    .text(
                        languageManager.getMessage("general.newUpdateAvailable"),
                    ).clickEvent(
                        ClickEvent.openUrl("https://github.com/m1sk9/LunaticChat/releases/latest"),
                    )
            }
        }

        // Send chat mode notification
        chatModeManager?.let { manager ->
            val chatMode = manager.getChatMode(player.uniqueId)
            val modeKey =
                when (chatMode) {
                    ChatMode.GLOBAL -> "chatmode.mode.global"
                    ChatMode.CHANNEL -> "chatmode.mode.channel"
                }
            val modeText = languageManager.getMessage(modeKey)
            val notification =
                languageManager.getMessage(
                    "chatmode.notification.login",
                    mapOf("mode" to modeText),
                )
            player.sendMessage(MessageFormatter.format(notification))
        }

        // Send channel notification if in a channel
        channelManager?.let { manager ->
            val context = manager.getPlayerChannelContext(player.uniqueId)
            context?.let {
                val notification =
                    MessageFormatter.format(
                        languageManager.getMessage(
                            "channel.notification.login",
                            mapOf("channelName" to it.channel.name),
                        ),
                    )
                player.sendMessage(notification)
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player
        val playerId = player.uniqueId

        // 1. Clear direct message references
        lunaticChat.directMessageHandler.clearPlayer(player)

        // 2. Clear active channel for this player
        channelManager?.setPlayerChannel(playerId, null)

        // 3. Trigger async save of chat mode data
        chatModeManager?.saveToDisk()

        // 4. Trigger async save of player settings
        playerSettingsManager.saveToDisk()
    }
}
