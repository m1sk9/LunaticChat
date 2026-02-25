package dev.m1sk9.lunaticChat.paper.command.setting

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class SettingKeyTest {
    @Test
    fun `fromString should resolve japanese key`() {
        val result = SettingKey.fromString("japanese")
        assertIs<SettingKey.Japanese>(result)
    }

    @Test
    fun `fromString should resolve notice key`() {
        val result = SettingKey.fromString("notice")
        assertIs<SettingKey.Notice>(result)
    }

    @Test
    fun `fromString should resolve chNotice key`() {
        val result = SettingKey.fromString("chNotice")
        assertIs<SettingKey.ChNotice>(result)
    }

    @Test
    fun `fromString should be case insensitive`() {
        assertIs<SettingKey.Japanese>(SettingKey.fromString("JAPANESE"))
        assertIs<SettingKey.Notice>(SettingKey.fromString("NOTICE"))
        assertIs<SettingKey.ChNotice>(SettingKey.fromString("CHNOTICE"))
        assertIs<SettingKey.Japanese>(SettingKey.fromString("Japanese"))
    }

    @Test
    fun `fromString should return null for unknown key`() {
        assertNull(SettingKey.fromString("unknown"))
        assertNull(SettingKey.fromString(""))
        assertNull(SettingKey.fromString("invalid"))
    }

    @Test
    fun `values should return all setting keys`() {
        val values = SettingKey.values()
        assertEquals(3, values.size)
        assertIs<SettingKey.Japanese>(values[0])
        assertIs<SettingKey.Notice>(values[1])
        assertIs<SettingKey.ChNotice>(values[2])
    }
}
