package dev.m1sk9.lunaticChat.paper.i18n

import kotlin.test.Test
import kotlin.test.assertEquals

class LanguageTest {
    @Test
    fun `should have correct code for EN`() {
        assertEquals("en", Language.EN.code)
    }

    @Test
    fun `should have correct code for JA`() {
        assertEquals("ja", Language.JA.code)
    }

    @Test
    fun `should have correct fileName for EN`() {
        assertEquals("en.yml", Language.EN.fileName)
    }

    @Test
    fun `should have correct fileName for JA`() {
        assertEquals("ja.yml", Language.JA.fileName)
    }

    @Test
    fun `fromCode should return EN for 'en'`() {
        assertEquals(Language.EN, Language.fromCode("en"))
    }

    @Test
    fun `fromCode should return JA for 'ja'`() {
        assertEquals(Language.JA, Language.fromCode("ja"))
    }

    @Test
    fun `fromCode should be case insensitive for EN`() {
        assertEquals(Language.EN, Language.fromCode("EN"))
        assertEquals(Language.EN, Language.fromCode("En"))
        assertEquals(Language.EN, Language.fromCode("eN"))
    }

    @Test
    fun `fromCode should be case insensitive for JA`() {
        assertEquals(Language.JA, Language.fromCode("JA"))
        assertEquals(Language.JA, Language.fromCode("Ja"))
        assertEquals(Language.JA, Language.fromCode("jA"))
    }

    @Test
    fun `fromCode should fallback to EN for unknown code`() {
        assertEquals(Language.EN, Language.fromCode("unknown"))
        assertEquals(Language.EN, Language.fromCode("fr"))
        assertEquals(Language.EN, Language.fromCode("de"))
        assertEquals(Language.EN, Language.fromCode(""))
    }

    @Test
    fun `fromCode should fallback to EN for empty string`() {
        assertEquals(Language.EN, Language.fromCode(""))
    }

    @Test
    fun `should have exactly two language entries`() {
        assertEquals(2, Language.entries.size)
    }

    @Test
    fun `entries should contain EN and JA`() {
        val entries = Language.entries
        assertEquals(true, entries.contains(Language.EN))
        assertEquals(true, entries.contains(Language.JA))
    }
}
