package dev.m1sk9.lunaticChat.engine.converter

import kotlin.test.Test
import kotlin.test.assertEquals

class KanaConverterTest {
    @Test
    fun `should convert single vowels to hiragana`() {
        assertEquals("あ", KanaConverter.toHiragana("a"))
        assertEquals("い", KanaConverter.toHiragana("i"))
        assertEquals("う", KanaConverter.toHiragana("u"))
        assertEquals("え", KanaConverter.toHiragana("e"))
        assertEquals("お", KanaConverter.toHiragana("o"))
    }

    @Test
    fun `should convert basic ka-row consonants to hiragana`() {
        assertEquals("か", KanaConverter.toHiragana("ka"))
        assertEquals("き", KanaConverter.toHiragana("ki"))
        assertEquals("く", KanaConverter.toHiragana("ku"))
        assertEquals("け", KanaConverter.toHiragana("ke"))
        assertEquals("こ", KanaConverter.toHiragana("ko"))
    }

    @Test
    fun `should convert simple word konnichiwa to hiragana`() {
        assertEquals("こんいちわ", KanaConverter.toHiragana("konnichiwa"))
    }

    @Test
    fun `should convert simple word arigatou to hiragana`() {
        assertEquals("ありがとう", KanaConverter.toHiragana("arigatou"))
    }

    @Test
    fun `should convert xtsu and ltsu to small tsu`() {
        assertEquals("っ", KanaConverter.toHiragana("xtsu"))
        assertEquals("っ", KanaConverter.toHiragana("ltsu"))
    }

    @Test
    fun `should convert xtu and ltu to small tsu`() {
        assertEquals("っ", KanaConverter.toHiragana("xtu"))
        assertEquals("っ", KanaConverter.toHiragana("ltu"))
    }

    @Test
    fun `should convert xa and la to small a`() {
        assertEquals("ぁ", KanaConverter.toHiragana("xa"))
        assertEquals("ぁ", KanaConverter.toHiragana("la"))
    }

    @Test
    fun `should convert xi and li to small i`() {
        assertEquals("ぃ", KanaConverter.toHiragana("xi"))
        assertEquals("ぃ", KanaConverter.toHiragana("li"))
    }

    @Test
    fun `should convert xu and lu to small u`() {
        assertEquals("ぅ", KanaConverter.toHiragana("xu"))
        assertEquals("ぅ", KanaConverter.toHiragana("lu"))
    }

    @Test
    fun `should convert xe and le to small e`() {
        assertEquals("ぇ", KanaConverter.toHiragana("xe"))
        assertEquals("ぇ", KanaConverter.toHiragana("le"))
    }

    @Test
    fun `should convert xo and lo to small o`() {
        assertEquals("ぉ", KanaConverter.toHiragana("xo"))
        assertEquals("ぉ", KanaConverter.toHiragana("lo"))
    }

    @Test
    fun `should convert xya and lya to small ya`() {
        assertEquals("ゃ", KanaConverter.toHiragana("xya"))
        assertEquals("ゃ", KanaConverter.toHiragana("lya"))
    }

    @Test
    fun `should convert xyu and lyu to small yu`() {
        assertEquals("ゅ", KanaConverter.toHiragana("xyu"))
        assertEquals("ゅ", KanaConverter.toHiragana("lyu"))
    }

    @Test
    fun `should convert xyo and lyo to small yo`() {
        assertEquals("ゃ", KanaConverter.toHiragana("xya"))
        assertEquals("ゃ", KanaConverter.toHiragana("lya"))
    }

    @Test
    fun `should convert xwa and lwa to small wa`() {
        assertEquals("ゎ", KanaConverter.toHiragana("xwa"))
        assertEquals("ゎ", KanaConverter.toHiragana("lwa"))
    }

    @Test
    fun `should convert user example - are kore op xtute raxtuka de shinanai question`() {
        assertEquals("って", KanaConverter.toHiragana("xtute"))
        assertEquals("らっか", KanaConverter.toHiragana("raxtuka"))

        val input = "are kore op xtute raxtuka de shinanai ?"
        val expected = "あれ これ おp って らっか で しなない ?"
        val actual = KanaConverter.toHiragana(input)

        assertEquals(expected, actual)
    }

    @Test
    fun `should convert xtute to tte using x prefix`() {
        assertEquals("って", KanaConverter.toHiragana("xtute"))
    }

    @Test
    fun `should convert raxtuka to rakka with small tsu`() {
        assertEquals("らっか", KanaConverter.toHiragana("raxtuka"))
    }

    @Test
    fun `should convert double consonants to small tsu`() {
        assertEquals("きって", KanaConverter.toHiragana("kitte"))
        assertEquals("がっこう", KanaConverter.toHiragana("gakkou"))
        assertEquals("ずっと", KanaConverter.toHiragana("zutto"))
        assertEquals("さっぱり", KanaConverter.toHiragana("sappari"))
    }

    @Test
    fun `should handle double t as small tsu`() {
        assertEquals("まった", KanaConverter.toHiragana("matta"))
    }

    @Test
    fun `should handle double k as small tsu`() {
        assertEquals("がっき", KanaConverter.toHiragana("gakki"))
    }

    @Test
    fun `should handle double p as small tsu`() {
        assertEquals("いっぱい", KanaConverter.toHiragana("ippai"))
    }

    @Test
    fun `should convert kya-row to hiragana`() {
        assertEquals("きゃ", KanaConverter.toHiragana("kya"))
        assertEquals("きゅ", KanaConverter.toHiragana("kyu"))
        assertEquals("きょ", KanaConverter.toHiragana("kyo"))
    }

    @Test
    fun `should convert sha-row to hiragana`() {
        assertEquals("しゃ", KanaConverter.toHiragana("sha"))
        assertEquals("しゅ", KanaConverter.toHiragana("shu"))
        assertEquals("しょ", KanaConverter.toHiragana("sho"))
    }

    @Test
    fun `should convert cha-row to hiragana`() {
        assertEquals("ちゃ", KanaConverter.toHiragana("cha"))
        assertEquals("ちゅ", KanaConverter.toHiragana("chu"))
        assertEquals("ちょ", KanaConverter.toHiragana("cho"))
    }

    @Test
    fun `should convert nya-row to hiragana`() {
        assertEquals("にゃ", KanaConverter.toHiragana("nya"))
        assertEquals("にゅ", KanaConverter.toHiragana("nyu"))
        assertEquals("にょ", KanaConverter.toHiragana("nyo"))
    }

    @Test
    fun `should convert hya-row to hiragana`() {
        assertEquals("ひゃ", KanaConverter.toHiragana("hya"))
        assertEquals("ひゅ", KanaConverter.toHiragana("hyu"))
        assertEquals("ひょ", KanaConverter.toHiragana("hyo"))
    }

    @Test
    fun `should convert mya-row to hiragana`() {
        assertEquals("みゃ", KanaConverter.toHiragana("mya"))
        assertEquals("みゅ", KanaConverter.toHiragana("myu"))
        assertEquals("みょ", KanaConverter.toHiragana("myo"))
    }

    @Test
    fun `should convert rya-row to hiragana`() {
        assertEquals("りゃ", KanaConverter.toHiragana("rya"))
        assertEquals("りゅ", KanaConverter.toHiragana("ryu"))
        assertEquals("りょ", KanaConverter.toHiragana("ryo"))
    }

    @Test
    fun `should convert gya-row to hiragana`() {
        assertEquals("ぎゃ", KanaConverter.toHiragana("gya"))
        assertEquals("ぎゅ", KanaConverter.toHiragana("gyu"))
        assertEquals("ぎょ", KanaConverter.toHiragana("gyo"))
    }

    @Test
    fun `should convert ja-row to hiragana`() {
        assertEquals("じゃ", KanaConverter.toHiragana("ja"))
        assertEquals("じ", KanaConverter.toHiragana("ji"))
        assertEquals("じゅ", KanaConverter.toHiragana("ju"))
        assertEquals("じょ", KanaConverter.toHiragana("jo"))
    }

    @Test
    fun `should return empty string for empty input`() {
        assertEquals("", KanaConverter.toHiragana(""))
    }

    @Test
    fun `should pass through non-romanji characters unchanged`() {
        assertEquals("123", KanaConverter.toHiragana("123"))
        assertEquals("!", KanaConverter.toHiragana("!"))
        assertEquals("?", KanaConverter.toHiragana("?"))
    }

    @Test
    fun `should handle mixed romanji and non-romanji`() {
        assertEquals("あ123い", KanaConverter.toHiragana("a123i"))
    }

    @Test
    fun `should handle uppercase input by converting to lowercase first`() {
        assertEquals("か", KanaConverter.toHiragana("KA"))
        assertEquals("きゃ", KanaConverter.toHiragana("KYA"))
    }

    @Test
    fun `should handle mixed case input`() {
        assertEquals("こんいちわ", KanaConverter.toHiragana("KoNnIcHiWa"))
    }

    @Test
    fun `should convert n to n-sound correctly`() {
        assertEquals("ん", KanaConverter.toHiragana("n"))
        assertEquals("ん", KanaConverter.toHiragana("nn"))
    }

    @Test
    fun `should handle tsu correctly`() {
        assertEquals("つ", KanaConverter.toHiragana("tsu"))
    }

    @Test
    fun `should handle chi and shi correctly`() {
        assertEquals("ち", KanaConverter.toHiragana("chi"))
        assertEquals("し", KanaConverter.toHiragana("shi"))
    }

    @Test
    fun `should handle fu correctly`() {
        assertEquals("ふ", KanaConverter.toHiragana("fu"))
        assertEquals("ふ", KanaConverter.toHiragana("hu"))
    }

    @Test
    fun `should handle wo and wa correctly`() {
        assertEquals("を", KanaConverter.toHiragana("wo"))
        assertEquals("わ", KanaConverter.toHiragana("wa"))
    }

    @Test
    fun `should convert common greeting ohayou`() {
        assertEquals("おはよう", KanaConverter.toHiragana("ohayou"))
    }

    @Test
    fun `should convert common word sumimasen`() {
        assertEquals("すみません", KanaConverter.toHiragana("sumimasen"))
    }

    @Test
    fun `should convert phrase with double consonants`() {
        assertEquals("がんばって", KanaConverter.toHiragana("ganbatte"))
    }

    @Test
    fun `should convert phrase with small ya-row`() {
        assertEquals("きゃべつ", KanaConverter.toHiragana("kyabetsu"))
    }

    @Test
    fun `should handle long phrase with various patterns`() {
        val input = "watashi wa nihongo wo benkyou shiteimasu"
        val expected = "わたし わ にほんご を べんきょう しています"
        assertEquals(expected, KanaConverter.toHiragana(input))
    }
}
