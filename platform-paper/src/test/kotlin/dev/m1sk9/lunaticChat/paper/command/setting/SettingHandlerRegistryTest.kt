package dev.m1sk9.lunaticChat.paper.command.setting

import dev.m1sk9.lunaticChat.engine.command.CommandResult
import dev.m1sk9.lunaticChat.paper.command.core.CommandContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingHandlerRegistryTest {
    private fun createMockHandler(settingKey: SettingKey): SettingHandler =
        object : SettingHandler {
            override val key: SettingKey = settingKey

            override fun execute(
                ctx: CommandContext,
                enable: Boolean,
            ): CommandResult = CommandResult.Success

            override fun showStatus(ctx: CommandContext): CommandResult = CommandResult.Success
        }

    @Test
    fun `register should make handler retrievable`() {
        val registry = SettingHandlerRegistry()
        val handler = createMockHandler(SettingKey.Japanese)

        registry.register(handler)

        assertEquals(handler, registry.getHandler(SettingKey.Japanese))
    }

    @Test
    fun `registerAll should register multiple handlers`() {
        val registry = SettingHandlerRegistry()
        val handler1 = createMockHandler(SettingKey.Japanese)
        val handler2 = createMockHandler(SettingKey.Notice)
        val handler3 = createMockHandler(SettingKey.ChNotice)

        registry.registerAll(handler1, handler2, handler3)

        assertEquals(handler1, registry.getHandler(SettingKey.Japanese))
        assertEquals(handler2, registry.getHandler(SettingKey.Notice))
        assertEquals(handler3, registry.getHandler(SettingKey.ChNotice))
    }

    @Test
    fun `getHandler should return null for unregistered key`() {
        val registry = SettingHandlerRegistry()

        assertNull(registry.getHandler(SettingKey.Japanese))
    }

    @Test
    fun `getAvailableKeys should return registered keys`() {
        val registry = SettingHandlerRegistry()
        registry.register(createMockHandler(SettingKey.Japanese))
        registry.register(createMockHandler(SettingKey.Notice))

        val keys = registry.getAvailableKeys()

        assertEquals(2, keys.size)
        assertTrue(keys.contains("japanese"))
        assertTrue(keys.contains("notice"))
    }

    @Test
    fun `getAvailableKeys should return empty list for empty registry`() {
        val registry = SettingHandlerRegistry()
        assertTrue(registry.getAvailableKeys().isEmpty())
    }

    @Test
    fun `register should overwrite handler with same key`() {
        val registry = SettingHandlerRegistry()
        val handler1 = createMockHandler(SettingKey.Japanese)
        val handler2 = createMockHandler(SettingKey.Japanese)

        registry.register(handler1)
        registry.register(handler2)

        assertEquals(handler2, registry.getHandler(SettingKey.Japanese))
        assertEquals(1, registry.getAvailableKeys().size)
    }

    @Test
    fun `register should return registry for method chaining`() {
        val registry = SettingHandlerRegistry()
        val result = registry.register(createMockHandler(SettingKey.Japanese))

        assertEquals(registry, result)
    }

    @Test
    fun `registerAll should return registry for method chaining`() {
        val registry = SettingHandlerRegistry()
        val result = registry.registerAll(createMockHandler(SettingKey.Japanese))

        assertEquals(registry, result)
    }
}
