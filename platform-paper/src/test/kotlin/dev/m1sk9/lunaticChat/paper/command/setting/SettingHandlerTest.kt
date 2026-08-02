package dev.m1sk9.lunaticChat.paper.command.setting

import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.paper.TestUtils
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import dev.m1sk9.lunaticChat.paper.i18n.LanguageManager
import dev.m1sk9.lunaticChat.paper.settings.PlayerSettingsManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.kyori.adventure.text.Component
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SettingHandlerTest {
    private val testUUID = UUID.fromString("00000001-0000-0000-0000-000000000000")

    private class Fixture(
        key: SettingKey,
        uuid: UUID,
    ) {
        val ctx = mockk<CommandContext>(relaxed = true)
        val settingsManager = mockk<PlayerSettingsManager>(relaxed = true)
        val languageManager = mockk<LanguageManager>(relaxed = true)
        val handler: SettingHandler

        init {
            every { ctx.requirePlayer() } returns TestUtils.createMockPlayer(uuid = uuid, name = "TestPlayer")
            every { settingsManager.getSettings(uuid) } returns TestUtils.createTestPlayerSettings(uuid = uuid)
            every { languageManager.getMessage(any(), any()) } returns "test message"
            every { languageManager.getToggleText(any()) } returns "ON"
            handler = SettingHandler(key, settingsManager, languageManager)
        }
    }

    private fun eachKey(assertion: (SettingKey, Fixture) -> Unit) =
        SettingKey.values().forEach { key -> assertion(key, Fixture(key, testUUID)) }

    @Test
    fun `execute enable writes the setting as enabled`() =
        eachKey { key, f ->
            assertIs<CommandResult.Success>(f.handler.execute(f.ctx, true))
            verify { f.settingsManager.updateSettings(match { key.read(it) }) }
        }

    @Test
    fun `execute disable writes the setting as disabled`() =
        eachKey { key, f ->
            assertIs<CommandResult.Success>(f.handler.execute(f.ctx, false))
            verify { f.settingsManager.updateSettings(match { !key.read(it) }) }
        }

    @Test
    fun `execute reports the change with the toggle message`() =
        eachKey { key, f ->
            f.handler.execute(f.ctx, true)

            val player = f.ctx.requirePlayer()
            verify { f.languageManager.getMessage(key.toggleMessageKey, mapOf("toggle" to "ON")) }
            verify { player.sendMessage(any<Component>()) }
        }

    @Test
    fun `showStatus reports the current value with the status message`() =
        eachKey { key, f ->
            assertIs<CommandResult.Success>(f.handler.showStatus(f.ctx))

            val player = f.ctx.requirePlayer()
            verify { f.languageManager.getMessage(key.statusMessageKey, mapOf("toggle" to "ON")) }
            verify { player.sendMessage(any<Component>()) }
        }

    @Test
    fun `handler exposes the key it was built for`() = eachKey { key, f -> assertEquals(key, f.handler.key) }

    @Test
    fun `each key round-trips through write and read`() {
        val settings = TestUtils.createTestPlayerSettings(uuid = testUUID)

        SettingKey.values().forEach { key ->
            assertTrue(key.read(key.write(settings, true)), key.key)
            assertFalse(key.read(key.write(settings, false)), key.key)
        }
    }

    @Test
    fun `writing one key leaves the other settings untouched`() {
        val original = TestUtils.createTestPlayerSettings(uuid = testUUID)

        SettingKey.values().forEach { key ->
            val flipped = key.write(original, !key.read(original))

            SettingKey.values().filterNot { it == key }.forEach { other ->
                assertEquals(
                    other.read(original),
                    other.read(flipped),
                    "${other.key} changed while writing ${key.key}",
                )
            }
        }
    }
}
