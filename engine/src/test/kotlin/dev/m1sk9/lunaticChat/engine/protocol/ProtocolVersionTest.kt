package dev.m1sk9.lunaticChat.engine.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProtocolVersionTest {
    @Test
    fun `version string should match MAJOR MINOR PATCH format`() {
        assertEquals("${ProtocolVersion.MAJOR}.${ProtocolVersion.MINOR}.${ProtocolVersion.PATCH}", ProtocolVersion.version)
    }

    @Test
    fun `isCompatible with matching major and minor should return true`() {
        assertTrue(ProtocolVersion.isCompatible(ProtocolVersion.MAJOR, ProtocolVersion.MINOR))
    }

    @Test
    fun `isCompatible with different major should return false`() {
        assertFalse(ProtocolVersion.isCompatible(ProtocolVersion.MAJOR + 1, ProtocolVersion.MINOR))
    }

    @Test
    fun `isCompatible with different minor should return false`() {
        assertFalse(ProtocolVersion.isCompatible(ProtocolVersion.MAJOR, ProtocolVersion.MINOR + 1))
    }

    @Test
    fun `isCompatible string with matching version should return true`() {
        assertTrue(ProtocolVersion.isCompatible("${ProtocolVersion.MAJOR}.${ProtocolVersion.MINOR}.0"))
    }

    @Test
    fun `isCompatible string with different patch should return true`() {
        assertTrue(ProtocolVersion.isCompatible("${ProtocolVersion.MAJOR}.${ProtocolVersion.MINOR}.99"))
    }

    @Test
    fun `isCompatible string with different major should return false`() {
        assertFalse(ProtocolVersion.isCompatible("${ProtocolVersion.MAJOR + 1}.${ProtocolVersion.MINOR}.0"))
    }

    @Test
    fun `isCompatible string with malformed version should return false`() {
        assertFalse(ProtocolVersion.isCompatible("invalid"))
        assertFalse(ProtocolVersion.isCompatible("1.0"))
        assertFalse(ProtocolVersion.isCompatible(""))
        assertFalse(ProtocolVersion.isCompatible("a.b.c"))
        assertFalse(ProtocolVersion.isCompatible("1.0.0.0"))
    }

    @Test
    fun `isCompatible string with two parts should return false`() {
        assertFalse(ProtocolVersion.isCompatible("1.0"))
    }
}
