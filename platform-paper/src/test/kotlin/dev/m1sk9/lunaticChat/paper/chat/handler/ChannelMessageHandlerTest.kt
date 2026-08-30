package dev.m1sk9.lunaticChat.paper.chat.handler

import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelContext
import dev.m1sk9.lunaticChat.engine.chat.channel.ChannelMessageLogEntry
import dev.m1sk9.lunaticChat.paper.TestUtils
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelManager
import dev.m1sk9.lunaticChat.paper.chat.channel.ChannelMessageLogger
import dev.m1sk9.lunaticChat.paper.common.SoundCollector
import dev.m1sk9.lunaticChat.paper.config.key.MessageFormatConfig
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.settings.PlayerSettingsManager
import io.ktor.util.logging.Logger
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import org.bukkit.Bukkit
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ChannelMessageHandlerTest {
    private val senderId = TestUtils.createTestUUID(1)
    private val memberId = TestUtils.createTestUUID(2)
    private val channel = TestUtils.createTestChannel(id = "ch-1", name = "general")

    private val messageFormats = TestUtils.createTestMessageFormats()
    private val channelManager = mockk<ChannelManager>(relaxed = true)
    private val languageManager = mockk<LanguageManager>(relaxed = true)
    private val logger = mockk<Logger>(relaxed = true)

    private val sender = TestUtils.createMockPlayer(uuid = senderId, name = "Alice")
    private val member = TestUtils.createMockPlayer(uuid = memberId, name = "Bob")

    @BeforeTest
    fun setUp() {
        mockkStatic(Bukkit::class)
        every { Bukkit.getPlayer(any<UUID>()) } returns null
        every { Bukkit.getPlayer(senderId) } returns sender
        every { Bukkit.getPlayer(memberId) } returns member
        every { channelManager.getPlayerChannelContext(senderId) } returns
            ChannelContext(
                channel = channel,
                members =
                    listOf(
                        TestUtils.createTestChannelMember(channelId = channel.id, playerId = senderId),
                        TestUtils.createTestChannelMember(channelId = channel.id, playerId = memberId),
                    ),
            )
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic(Bukkit::class)
    }

    private fun handler(
        settingsManager: PlayerSettingsManager? = null,
        messageLogger: ChannelMessageLogger? = null,
    ) = ChannelMessageHandler(messageFormats, settingsManager, channelManager, languageManager, messageLogger, logger)

    @Test
    fun `a player with no active channel is not handled`() {
        every { channelManager.getPlayerChannelContext(senderId) } returns null

        assertFalse(handler().sendChannelMessage(sender, "hello"))
        verify(exactly = 0) { member.sendMessage(any<Component>()) }
    }

    @Test
    fun `every online member receives the message in the configured channel format`() {
        val delivered = slot<Component>()

        assertTrue(handler().sendChannelMessage(sender, "hello"))

        verify { member.sendMessage(capture(delivered)) }
        assertEquals("§7[§b#general§7] §eAlice: §fhello", assertIs<TextComponent>(delivered.captured).content())
    }

    @Test
    fun `a format replaced while the server runs applies to the next message`() {
        messageFormats.replace(MessageFormatConfig(channelMessageFormat = "<{channel}> {sender}: {message}"))
        val delivered = slot<Component>()

        handler().sendChannelMessage(sender, "hello")

        verify { member.sendMessage(capture(delivered)) }
        assertEquals("<general> Alice: hello", assertIs<TextComponent>(delivered.captured).content())
    }

    @Test
    fun `a member who went offline receives nothing`() {
        val offline = TestUtils.createMockPlayer(uuid = memberId, name = "Bob", isOnline = false)
        every { Bukkit.getPlayer(memberId) } returns offline

        assertTrue(handler().sendChannelMessage(sender, "hello"))

        verify(exactly = 0) { offline.sendMessage(any<Component>()) }
    }

    @Test
    fun `a member who left the server receives nothing`() {
        every { Bukkit.getPlayer(memberId) } returns null

        assertTrue(handler().sendChannelMessage(sender, "hello"))

        verify(exactly = 0) { member.sendMessage(any<Component>()) }
    }

    @Test
    fun `the sender hears the send notification when it is enabled`() {
        val settingsManager = mockk<PlayerSettingsManager>()
        every { settingsManager.getSettings(any()) } returns
            TestUtils.createTestPlayerSettings(channelMessageNotificationEnabled = true)

        handler(settingsManager = settingsManager).sendChannelMessage(sender, "hello")

        verify { sender.playSound(SoundCollector.LUNATIC_POP_SOUND) }
    }

    @Test
    fun `the send notification is silent when the sender disabled it`() {
        val settingsManager = mockk<PlayerSettingsManager>()
        every { settingsManager.getSettings(any()) } returns
            TestUtils.createTestPlayerSettings(channelMessageNotificationEnabled = false)

        handler(settingsManager = settingsManager).sendChannelMessage(sender, "hello")

        verify(exactly = 0) { sender.playSound(SoundCollector.LUNATIC_POP_SOUND) }
    }

    @Test
    fun `only members other than the sender hear the receive notification`() {
        val settingsManager = mockk<PlayerSettingsManager>()
        every { settingsManager.getSettings(any()) } returns
            TestUtils.createTestPlayerSettings(channelMessageNotificationEnabled = true)

        handler(settingsManager = settingsManager).sendChannelMessage(sender, "hello")

        verify { member.playSound(SoundCollector.LUNATIC_BELL_SOUND) }
        verify(exactly = 0) { sender.playSound(SoundCollector.LUNATIC_BELL_SOUND) }
    }

    @Test
    fun `no notification is played when player settings are unavailable`() {
        handler().sendChannelMessage(sender, "hello")

        verify(exactly = 0) { sender.playSound(any<Sound>()) }
        verify(exactly = 0) { member.playSound(any<Sound>()) }
    }

    @Test
    fun `the message is written to the channel log`() {
        val messageLogger = mockk<ChannelMessageLogger>(relaxed = true)
        val entry = slot<ChannelMessageLogEntry>()

        handler(messageLogger = messageLogger).sendChannelMessage(sender, "hello")

        verify { messageLogger.logMessage(capture(entry)) }
        assertEquals(senderId.toString(), entry.captured.playerId)
        assertEquals("Alice", entry.captured.playerName)
        assertEquals(channel.id, entry.captured.channelId)
        assertEquals("hello", entry.captured.message)
    }
}
