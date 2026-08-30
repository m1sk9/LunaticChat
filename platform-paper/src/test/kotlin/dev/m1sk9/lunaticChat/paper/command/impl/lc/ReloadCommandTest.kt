package dev.m1sk9.lunaticChat.paper.command.impl.lc

import com.charleskorn.kaml.Yaml
import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.TestUtils
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.config.ConfigManager
import dev.m1sk9.lunaticChat.paper.config.ConfigurationReloader
import dev.m1sk9.lunaticChat.paper.config.LunaticChatConfiguration
import dev.m1sk9.lunaticChat.paper.config.MessageFormatHolder
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.papermc.paper.command.brigadier.CommandSourceStack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@Suppress("UnstableApiUsage")
class ReloadCommandTest {
    private val startup = TestUtils.createTestConfiguration()
    private val holder = MessageFormatHolder(startup.messageFormat)
    private val languageManager =
        mockk<LanguageManager>(relaxed = true).also {
            every { it.getMessage(any(), any()) } answers { firstArg() }
        }

    private fun commandReading(contents: () -> String) =
        ReloadCommand(
            plugin = mockk<LunaticChat>(relaxed = true),
            languageManager = languageManager,
            reloader =
                ConfigurationReloader(
                    configManager = ConfigManager(TestUtils.TestLogger()),
                    startupConfiguration = startup,
                    messageFormatHolder = holder,
                    logger = TestUtils.TestLogger(),
                    readConfigFile = contents,
                ),
        )

    /** A console sender: no player behind it, which the command must never require. */
    private fun consoleContext(): CommandContext = mockk<CommandContext>(relaxed = true)

    private fun sourceWith(
        permission: String,
        granted: Boolean,
    ): CommandSourceStack =
        mockk<CommandSourceStack>().also {
            every { it.sender.hasPermission(permission) } returns granted
        }

    @Test
    fun `the node is gated on the reload permission`() {
        val node = commandReading { "" }.buildAll().single()

        assertTrue(node.requirement.test(sourceWith("lunaticchat.command.lc.reload", granted = true)))
        assertFalse(node.requirement.test(sourceWith("lunaticchat.command.lc.reload", granted = false)))
    }

    @Test
    fun `an applied change reports success, without naming the settings`() {
        val ctx = consoleContext()

        val result =
            commandReading {
                Yaml.default.encodeToString(
                    LunaticChatConfiguration.serializer(),
                    startup.copy(messageFormat = startup.messageFormat.copy(directMessageFormat = "new {message}")),
                )
            }.execute(ctx)

        assertIs<CommandResult.SuccessWithMessage>(result)
        assertEquals("reload.success", result.message)
        assertEquals("new {message}", holder.current.directMessageFormat)
        // The one message handleResult sends is the whole reply; no per-setting lines.
        verify(exactly = 0) { ctx.reply(any()) }
    }

    @Test
    fun `a change that cannot take effect asks for a restart`() {
        val result =
            commandReading {
                Yaml.default.encodeToString(
                    LunaticChatConfiguration.serializer(),
                    startup.copy(features = startup.features.copy(channelChat = startup.features.channelChat.copy(enabled = true))),
                )
            }.execute(consoleContext())

        assertIs<CommandResult.SuccessWithMessage>(result)
        assertEquals("reload.restartRequired", result.message)
    }

    @Test
    fun `a change needing a restart is not reported as no change, even when nothing was applied`() {
        // Nothing moved, but the operator did edit the file: telling them there was no change would
        // send them hunting for a typo they had not made.
        val result =
            commandReading {
                Yaml.default.encodeToString(LunaticChatConfiguration.serializer(), startup.copy(debug = !startup.debug))
            }.execute(consoleContext())

        assertIs<CommandResult.SuccessWithMessage>(result)
        assertEquals("reload.restartRequired", result.message)
        assertEquals(startup.messageFormat, holder.current)
    }

    @Test
    fun `a file with nothing to apply says so instead of claiming success`() {
        val unchanged = Yaml.default.encodeToString(LunaticChatConfiguration.serializer(), startup)

        val result = commandReading { unchanged }.execute(consoleContext())

        assertIs<CommandResult.SuccessWithMessage>(result)
        assertEquals("reload.noChanges", result.message)
    }

    @Test
    fun `an unreadable setting fails the command and names the setting`() {
        val ctx = consoleContext()

        val result = commandReading { "debug: maybe" }.execute(ctx)

        assertIs<CommandResult.Failure>(result)
        assertEquals("reload.previousKept", result.message)
        assertEquals(startup.messageFormat, holder.current)
        // The headline plus one line per rejected setting.
        verify(exactly = 2) { ctx.reply(any()) }
    }

    @Test
    fun `a document that is not a settings file fails the command`() {
        val result = commandReading { "# only a comment" }.execute(consoleContext())

        assertIs<CommandResult.Failure>(result)
        assertEquals(startup.messageFormat, holder.current)
    }

    @Test
    fun `a file that cannot be read fails the command`() {
        val result = commandReading { throw java.io.IOException("gone") }.execute(consoleContext())

        assertIs<CommandResult.Failure>(result)
        assertEquals(startup.messageFormat, holder.current)
    }
}
