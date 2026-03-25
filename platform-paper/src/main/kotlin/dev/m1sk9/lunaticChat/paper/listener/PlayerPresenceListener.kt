package dev.m1sk9.lunaticChat.paper.listener

import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.paper.BuildInfo
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelManager
import dev.m1sk9.lunaticChat.paper.common.hasAnyPermission
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.i18n.MessageFormatter
import dev.m1sk9.lunaticChat.paper.settings.PlayerSettingsManager
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class PlayerPresenceListener(
    private val lunaticChat: LunaticChat,
    private val languageManager: LanguageManager,
    private val updateCheckerFlag: AtomicBoolean,
    private val playerSettingsManager: PlayerSettingsManager,
    private val channelManager: ChannelManager? = null,
) : Listener {
    @EventHandler(ignoreCancelled = true)
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player

        // Send update notification if available
        if (updateCheckerFlag.get() && player.hasAnyPermission { +LunaticChatPermissionNode.NoticeUpdate }) {
            lunaticChat.server.asyncScheduler.runDelayed(lunaticChat, { _ ->
                if (player.isOnline) {
                    player.sendMessage(
                        MessageFormatter
                            .format(languageManager.getMessage("general.newUpdateAvailable"))
                            .clickEvent(ClickEvent.openUrl("https://github.com/m1sk9/LunaticChat/releases/latest")),
                    )
                }
            }, 3, TimeUnit.SECONDS)
        }

        // Nightly build warning
        if (BuildInfo.isNightly) {
            lunaticChat.server.asyncScheduler.runDelayed(lunaticChat, { _ ->
                if (player.isOnline) {
                    player.sendMessage(
                        MessageFormatter
                            .format(languageManager.getMessage("general.nightlyWarning"))
                            .color(NamedTextColor.YELLOW),
                    )
                    player.sendMessage(
                        MessageFormatter
                            .format(languageManager.getMessage("general.nightlyReportIssue"))
                            .clickEvent(ClickEvent.openUrl("https://github.com/m1sk9/LunaticChat/issues"))
                            .color(NamedTextColor.YELLOW),
                    )
                }
            }, 6, TimeUnit.SECONDS)
        }

        // Restore active channel from membership and notify (delayed to avoid being buried by other plugins' join messages)
        channelManager?.let { manager ->
            val context = manager.restorePlayerChannel(player.uniqueId)
            context?.let {
                val notification =
                    MessageFormatter
                        .format(
                            languageManager.getMessage(
                                "channel.notification.login",
                                mapOf("channelName" to it.channel.name),
                            ),
                        ).clickEvent(
                            ClickEvent.runCommand("/lc channel status"),
                        )
                lunaticChat.server.asyncScheduler.runDelayed(lunaticChat, { _ ->
                    if (player.isOnline) {
                        player.sendMessage(notification)
                    }
                }, 3, TimeUnit.SECONDS)
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

        // 3. Trigger async save of player settings
        playerSettingsManager.saveToDisk()
    }
}
