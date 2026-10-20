package dev.m1sk9.lunaticChat.paper.common

import dev.m1sk9.lunaticChat.engine.permission.LunaticChatPermissionNode
import dev.m1sk9.lunaticChat.paper.TestUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class SpyPermissionManagerTest {
    private val onlinePlayers = mutableListOf<Player>()

    @BeforeTest
    fun setUp() {
        mockkStatic(Bukkit::class)
        every { Bukkit.getOnlinePlayers() } answers { onlinePlayers }
        SpyPermissionManager.updateSpyCache()
    }

    @AfterTest
    fun tearDown() {
        onlinePlayers.clear()
        SpyPermissionManager.updateSpyCache()
        unmockkStatic(Bukkit::class)
    }

    private fun player(
        name: String,
        spy: Boolean,
    ): Player {
        val player = TestUtils.createMockPlayer(name = name)
        player.grantSpy(spy)
        return player
    }

    private fun Player.grantSpy(granted: Boolean) {
        every { hasPermission(LunaticChatPermissionNode.Spy.permissionNode) } returns granted
    }

    private fun join(player: Player) {
        val event = mockk<PlayerJoinEvent>()
        every { event.player } returns player
        SpyPermissionManager.onPlayerJoin(event)
    }

    private fun sendDirectMessage() {
        SpyPermissionManager.notifySpies(noticeText = { "spy" }, exclude = { false }) { Component.text("dm") }
    }

    @Test
    fun `a joining player with the spy permission receives copies`() {
        val spy = player("Spy", spy = true)

        join(spy)
        sendDirectMessage()

        verify(exactly = 1) { spy.sendMessage(any<Component>()) }
    }

    @Test
    fun `a joining player without the spy permission receives no copies`() {
        val player = player("Player", spy = false)

        join(player)
        sendDirectMessage()

        verify(exactly = 0) { player.sendMessage(any<Component>()) }
    }

    @Test
    fun `a join evaluates only the joining player`() {
        val online = player("Online", spy = false)
        onlinePlayers += online

        join(player("Joining", spy = false))

        verify(exactly = 0) { online.hasPermission(any<String>()) }
    }

    @Test
    fun `a spy whose permission is revoked while online stops receiving copies`() {
        val spy = player("Spy", spy = true)
        join(spy)

        spy.grantSpy(false)
        sendDirectMessage()

        verify(exactly = 0) { spy.sendMessage(any<Component>()) }
    }

    @Test
    fun `updateSpyCache covers spies who were online before the cache was built`() {
        val spy = player("Spy", spy = true)
        onlinePlayers += spy

        SpyPermissionManager.updateSpyCache()
        sendDirectMessage()

        verify(exactly = 1) { spy.sendMessage(any<Component>()) }
    }
}
