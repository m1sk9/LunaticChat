package dev.m1sk9.lunaticChat.paper.command.impl.lc

import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.paper.LunaticChat
import dev.m1sk9.lunaticChat.paper.debug.DiagnosticsReport
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import io.mockk.every
import io.mockk.mockk
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

    @Test
    fun `a written report is reported as a success, naming the folder rather than the path`() {
        // The message is the whole reply: nothing carries where the file is, because the command
        // can be granted to someone with no filesystem access.
        every { report.write() } returns Path.of("plugins", "LunaticChat", "debug", "report-20260830-120000.txt")

        val result = command.execute()

        assertIs<CommandResult.SuccessWithMessage>(result)
        assertEquals("dump.success", result.message)
    }

    @Test
    fun `a report that cannot be written fails without taking the server down`() {
        every { report.write() } throws IOException("read-only file system")

        val result = command.execute()

        assertIs<CommandResult.Failure>(result)
        assertEquals("dump.failed", result.message)
    }
}
