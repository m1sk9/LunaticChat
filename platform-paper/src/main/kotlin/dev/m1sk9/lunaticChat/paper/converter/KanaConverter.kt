package dev.m1sk9.lunaticChat.paper.converter

/**
 * Converts romanji text to hiragana using Trie data structure.
 */
object KanaConverter {
    sealed class TrieNode {
        data class Leaf(val value: String) : TrieNode()
        data class Branch(val children: Map<Char, TrieNode>, val value: String? = null) : TrieNode()
    }

    private val romanjiTrie: TrieNode = buildTrie()

    private fun buildTrie(): TrieNode {
        val mappings = listOf(
            // 3文字変換
            "kya" to "きゃ", "kyi" to "きぃ", "kyu" to "きゅ", "kye" to "きぇ", "kyo" to "きょ",
            "gya" to "ぎゃ", "gyi" to "ぎぃ", "gyu" to "ぎゅ", "gye" to "ぎぇ", "gyo" to "ぎょ",
            "sha" to "しゃ", "shi" to "し", "shu" to "しゅ", "she" to "しぇ", "sho" to "しょ",
            "sya" to "しゃ", "syi" to "しぃ", "syu" to "しゅ", "sye" to "しぇ", "syo" to "しょ",
            "zya" to "じゃ", "zyi" to "じぃ", "zyu" to "じゅ", "zye" to "じぇ", "zyo" to "じょ",
            "jya" to "じゃ", "jyi" to "じぃ", "jyu" to "じゅ", "jye" to "じぇ", "jyo" to "じょ",
            "cha" to "ちゃ", "chi" to "ち", "chu" to "ちゅ", "che" to "ちぇ", "cho" to "ちょ",
            "tya" to "ちゃ", "tyi" to "ちぃ", "tyu" to "ちゅ", "tye" to "ちぇ", "tyo" to "ちょ",
            "dya" to "ぢゃ", "dyi" to "ぢぃ", "dyu" to "ぢゅ", "dye" to "ぢぇ", "dyo" to "ぢょ",
            "nya" to "にゃ", "nyi" to "にぃ", "nyu" to "にゅ", "nye" to "にぇ", "nyo" to "にょ",
            "hya" to "ひゃ", "hyi" to "ひぃ", "hyu" to "ひゅ", "hye" to "ひぇ", "hyo" to "ひょ",
            "bya" to "びゃ", "byi" to "びぃ", "byu" to "びゅ", "bye" to "びぇ", "byo" to "びょ",
            "pya" to "ぴゃ", "pyi" to "ぴぃ", "pyu" to "ぴゅ", "pye" to "ぴぇ", "pyo" to "ぴょ",
            "mya" to "みゃ", "myi" to "みぃ", "myu" to "みゅ", "mye" to "みぇ", "myo" to "みょ",
            "rya" to "りゃ", "ryi" to "りぃ", "ryu" to "りゅ", "rye" to "りぇ", "ryo" to "りょ",
            "tsu" to "つ", "thi" to "てぃ",
            "dhi" to "でぃ", "dhu" to "でゅ",
            "wha" to "うぁ", "whi" to "うぃ", "whe" to "うぇ", "who" to "うぉ",

            // 2文字変換
            "ka" to "か", "ki" to "き", "ku" to "く", "ke" to "け", "ko" to "こ",
            "ga" to "が", "gi" to "ぎ", "gu" to "ぐ", "ge" to "げ", "go" to "ご",
            "sa" to "さ", "si" to "し", "su" to "す", "se" to "せ", "so" to "そ",
            "za" to "ざ", "zi" to "じ", "zu" to "ず", "ze" to "ぜ", "zo" to "ぞ",
            "ja" to "じゃ", "ji" to "じ", "ju" to "じゅ", "je" to "じぇ", "jo" to "じょ",
            "ta" to "た", "ti" to "ち", "tu" to "つ", "te" to "て", "to" to "と",
            "da" to "だ", "di" to "ぢ", "du" to "づ", "de" to "で", "do" to "ど",
            "na" to "な", "ni" to "に", "nu" to "ぬ", "ne" to "ね", "no" to "の",
            "ha" to "は", "hi" to "ひ", "hu" to "ふ", "he" to "へ", "ho" to "ほ",
            "fu" to "ふ",
            "ba" to "ば", "bi" to "び", "bu" to "ぶ", "be" to "べ", "bo" to "ぼ",
            "pa" to "ぱ", "pi" to "ぴ", "pu" to "ぷ", "pe" to "ぺ", "po" to "ぽ",
            "ma" to "ま", "mi" to "み", "mu" to "む", "me" to "め", "mo" to "も",
            "ya" to "や", "yi" to "い", "yu" to "ゆ", "ye" to "いぇ", "yo" to "よ",
            "ra" to "ら", "ri" to "り", "ru" to "る", "re" to "れ", "ro" to "ろ",
            "wa" to "わ", "wi" to "ゐ", "wu" to "う", "we" to "ゑ", "wo" to "を",
            "la" to "ら", "li" to "り", "lu" to "る", "le" to "れ", "lo" to "ろ",
            "nn" to "ん",

            // 1文字変換
            "a" to "あ", "i" to "い", "u" to "う", "e" to "え", "o" to "お",
            "n" to "ん"
        )

        return insertAll(TrieNode.Branch(emptyMap()), mappings)
    }

    private fun insertAll(
        root: TrieNode,
        mappings: List<Pair<String, String>>,
    ): TrieNode {
        var current = root
        for ((key, value) in mappings) {
            current = insert(current, key, value)
        }
        return current
    }

    private fun insert(
        node: TrieNode,
        key: String,
        value: String,
    ): TrieNode {
        if (key.isEmpty()) {
            return when (node) {
                is TrieNode.Branch -> TrieNode.Branch(node.children, value)
                is TrieNode.Leaf -> TrieNode.Leaf(value)
            }
        }

        return when (node) {
            is TrieNode.Branch -> {
                val char = key[0]
                val child = node.children[char] ?: TrieNode.Branch(emptyMap())
                val newChild = insert(child, key.substring(1), value)
                TrieNode.Branch(node.children + (char to newChild), node.value)
            }
            is TrieNode.Leaf -> node
        }
    }

    /**
     * Converts romanji text to hiragana.
     *
     * @param input The romanji text to convert
     * @return The converted hiragana text
     */
    fun toHiragana(input: String): String {
        val result = StringBuilder()
        var i = 0
        val lowerInput = input.lowercase()

        while (i < lowerInput.length) {
            if (i + 1 < lowerInput.length) {
                val current = lowerInput[i]
                val next = lowerInput[i + 1]
                if (current == next && current in "bcdfghjklmpqrstvwxyz") {
                    result.append('っ')
                    i++
                    continue
                }
            }

            var node: TrieNode = romanjiTrie
            var lastMatch: Pair<String, Int>? = null
            var j = i

            while (j < lowerInput.length) {
                node = when (node) {
                    is TrieNode.Branch -> {
                        if (node.value != null) {
                            lastMatch = node.value to (j - i)
                        }

                        node.children[lowerInput[j]] ?: break
                    }
                    is TrieNode.Leaf -> {
                        lastMatch = node.value to (j - i)
                        break
                    }
                }
                j++
            }

            if (node is TrieNode.Leaf) {
                lastMatch = node.value to (j - i)
            } else if (node is TrieNode.Branch && node.value != null) {
                lastMatch = node.value to (j - i)
            }

            if (lastMatch != null) {
                result.append(lastMatch.first)
                i += lastMatch.second
            } else {
                result.append(lowerInput[i])
                i++
            }
        }

        return result.toString()
    }
}
