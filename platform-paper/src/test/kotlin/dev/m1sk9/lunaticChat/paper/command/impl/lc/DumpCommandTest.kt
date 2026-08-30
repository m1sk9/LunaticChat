package dev.m1sk9.lunaticChat.paper.command.impl.lc

import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.debug.DiagnosticsReport
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import java.io.File
import java.io.IOException
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DumpCommandTest {
    private val languageManager =
        mockk<LanguageManager>(relaxed = true) {
            every { getMessage(any(), any()) } answers { firstArg() }
            every { getMessage(any()) } answers { firstArg() }
        }

    private val report = mockk<DiagnosticsReport>()

    private val command = DumpCommand(mockk<LunaticChat>(relaxed = true), languageManager, report)

    /** A console sender: reports are an operator's job, so this must never require a player. */
    private fun consoleContext(): CommandContext = mockk(relaxed = true)

    @Test
    fun `a written report is reported with the path it landed in`() {
        every { report.write() } returns Path.of("plugins", "LunaticChat", "debug", "report-20260830-120000.txt")
        val ctx = consoleContext()

        val result = command.execute(ctx)

        assertIs<CommandResult.SuccessWithMessage>(result)
        assertEquals("dump.success", result.message)
        verify(exactly = 1) { ctx.reply(any<Component>()) }
    }

    @Test
    fun `the reply names the report relative to the server directory, not by its absolute path`() {
        // The command can be granted to someone with no filesystem access, to whom the absolute
        // path says only where the server is installed.
        every { report.write() } returns Path.of("plugins", "LunaticChat", "debug", "report-20260830-120000.txt")
        val replied = slot<Component>()
        val ctx = mockk<CommandContext>(relaxed = true)
        every { ctx.reply(capture(replied)) } returns Unit

        command.execute(ctx)

        val text = PlainTextComponentSerializer.plainText().serialize(replied.captured)
        assertEquals(
            "  • plugins/LunaticChat/debug/report-20260830-120000.txt".replace("/", File.separator),
            text,
        )
    }

    @Test
    fun `a report written outside the server directory is named by its file alone`() {
        // Relativizing would only produce a chain of `..` segments, which discloses the layout the
        // absolute path did without being any more usable.
        every { report.write() } returns Path.of("/", "var", "tmp", "report-20260830-120000.txt")
        val replied = slot<Component>()
        val ctx = mockk<CommandContext>(relaxed = true)
        every { ctx.reply(capture(replied)) } returns Unit

        command.execute(ctx)

        val text = PlainTextComponentSerializer.plainText().serialize(replied.captured)
        assertEquals("  • report-20260830-120000.txt", text)
    }

    @Test
    fun `a report that cannot be written fails without taking the server down`() {
        every { report.write() } throws IOException("read-only file system")

        val result = command.execute(consoleContext())

        assertIs<CommandResult.Failure>(result)
        assertEquals("dump.failed", result.message)
    }
}
