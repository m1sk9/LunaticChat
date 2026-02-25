package dev.m1sk9.lunaticChat.engine.chat

import kotlin.test.Test
import kotlin.test.assertEquals

class ChatModeTest {
    @Test
    fun `toggle should switch GLOBAL to CHANNEL`() {
        assertEquals(ChatMode.CHANNEL, ChatMode.GLOBAL.toggle())
    }

    @Test
    fun `toggle should switch CHANNEL to GLOBAL`() {
        assertEquals(ChatMode.GLOBAL, ChatMode.CHANNEL.toggle())
    }

    @Test
    fun `DEFAULT should be GLOBAL`() {
        assertEquals(ChatMode.GLOBAL, ChatMode.DEFAULT)
    }

    @Test
    fun `double toggle should return original mode`() {
        assertEquals(ChatMode.GLOBAL, ChatMode.GLOBAL.toggle().toggle())
        assertEquals(ChatMode.CHANNEL, ChatMode.CHANNEL.toggle().toggle())
    }

    @Test
    fun `enum should have exactly two values`() {
        assertEquals(2, ChatMode.entries.size)
        assertEquals(setOf(ChatMode.GLOBAL, ChatMode.CHANNEL), ChatMode.entries.toSet())
    }
}
