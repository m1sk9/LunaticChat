package dev.m1sk9.lunaticChat.paper.listener

import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.common.hasAnyPermission
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.concurrent.atomic.AtomicBoolean

class PlayerPresenceListener(
    private val lunaticChat: LunaticChat,
    private val updateCheckerFlag: AtomicBoolean,
) : Listener {
    @EventHandler(ignoreCancelled = true)
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (!updateCheckerFlag.get() || !player.hasAnyPermission { +LunaticChatPermissionNode.NoticeUpdate }) return

        player.sendMessage {
            Component
                .text(
                    listOf(
                        "§6[§eLunaticChat§6] §aA new update is available!",
                        "§aYou can download the latest build from §bGitHub §aor §bModrinth.",
                    ).joinToString("\n"),
                ).clickEvent(
                    ClickEvent.openUrl("https://modrinth.com/plugin/lunaticchat/version/latest"),
                )
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onQuit(event: PlayerQuitEvent) {
        lunaticChat.directMessageHandler.clearPlayer(event.player)
    }
}
