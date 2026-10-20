package dev.m1sk9.lunaticChat.engine.protocol

import kotlin.test.Test
import kotlin.test.assertEquals

class HexPreviewTest {
    @Test
    fun `bytes are rendered as zero-padded lowercase hex separated by spaces`() {
        assertEquals("00 0f ff", byteArrayOf(0x00, 0x0f, -1).toHexPreview())
    }

    @Test
    fun `only the first limit bytes are rendered`() {
        assertEquals("01 02", byteArrayOf(1, 2, 3).toHexPreview(limit = 2))
    }

    @Test
    fun `an empty payload renders as an empty string`() {
        assertEquals("", ByteArray(0).toHexPreview())
    }
}
