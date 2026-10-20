package dev.m1sk9.lunaticChat.paper.chat.handler

import dev.m1sk9.lunaticChat.paper.TestUtils
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelManager
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class ChannelNotificationHandlerTest {
    private val onlineId = TestUtils.createTestUUID(1)
    private val offlineId = TestUtils.createTestUUID(2)
    private val channel = TestUtils.createTestChannel(id = "ch-1", name = "general")

    private val channelManager = mockk<ChannelManager>()
    private val languageManager = mockk<LanguageManager>()
    private val online = TestUtils.createMockPlayer(uuid = onlineId, name = "Alice")

    @BeforeTest
    fun setUp() {
        mockkStatic(Bukkit::class)
        every { Bukkit.getPlayer(any<UUID>()) } returns null
        every { Bukkit.getPlayer(onlineId) } returns online
        every { channelManager.getChannel(channel.id) } returns Result.success(channel)
        every { channelManager.getChannelMembers(channel.id) } returns
            Result.success(
                listOf(
                    TestUtils.createTestChannelMember(channelId = channel.id, playerId = onlineId),
                    TestUtils.createTestChannelMember(channelId = channel.id, playerId = offlineId),
                ),
            )
        every { languageManager.getMessage("channel.notification.playerJoined", any()) } answers
            { "${secondArg<Map<String, String>>()["player"]} joined ${secondArg<Map<String, String>>()["channel"]}" }
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic(Bukkit::class)
    }

    @Test
    fun `a broadcast reaches online members with the channel name filled in and skips offline ones`() {
        ChannelNotificationHandler(channelManager, languageManager).broadcastJoin(channel.id, "Bob")

        verify(exactly = 1) {
            online.sendMessage(
                match<Component> { PlainTextComponentSerializer.plainText().serialize(it) == "[LC] Bob joined general" },
            )
        }
        verify(exactly = 1) { Bukkit.getPlayer(offlineId) }
    }

    @Test
    fun `a broadcast to a channel that no longer exists sends nothing`() {
        every { channelManager.getChannel(channel.id) } returns Result.failure(IllegalStateException())

        ChannelNotificationHandler(channelManager, languageManager).broadcastJoin(channel.id, "Bob")

        verify(exactly = 0) { online.sendMessage(any<Component>()) }
    }
}
