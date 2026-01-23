package dev.m1sk9.lunaticChat.paper.listener

import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.common.hasAnyPermission
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
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
) : Listener {
    @EventHandler(ignoreCancelled = true)
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (!updateCheckerFlag.get() || !player.hasAnyPermission { +LunaticChatPermissionNode.NoticeUpdate }) return

        player.sendMessage {
            Component
                .text(
                    languageManager.getMessage("newUpdateAvailable"),
                ).clickEvent(
                    ClickEvent.openUrl("https://github.com/m1sk9/LunaticChat/releases/latest"),
                )
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onQuit(event: PlayerQuitEvent) {
        lunaticChat.directMessageHandler.clearPlayer(event.player)
    }
}
