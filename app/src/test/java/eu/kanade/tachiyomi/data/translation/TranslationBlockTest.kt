package eu.kanade.tachiyomi.data.translation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TranslationBlockTest {

    @Test
    fun dropsTheShorterCopyWhenABubbleWasDetectedTwice() {
        val full = block("Hello there", 0.10f, 0.10f, 0.40f, 0.20f)
        val fragment = block("Hello", 0.12f, 0.12f, 0.30f, 0.15f)

        val kept = dedupeOverlappingBlocks(listOf(fragment, full))

        assertEquals(listOf(full), kept)
    }

    @Test
    fun keepsBubblesThatDoNotOverlap() {
        val top = block("One", 0.10f, 0.10f, 0.20f, 0.10f)
        val bottom = block("Two", 0.10f, 0.80f, 0.20f, 0.10f)

        val kept = dedupeOverlappingBlocks(listOf(top, bottom))

        assertEquals(setOf(top, bottom), kept.toSet())
    }

    @Test
    fun readsChineseScriptsFromLanguageCodes() {
        assertEquals(ChineseScript.TRADITIONAL, ChineseScriptConverter.scriptOf("zh-Hant"))
        assertEquals(ChineseScript.TRADITIONAL, ChineseScriptConverter.scriptOf("zh-TW"))
        assertEquals(ChineseScript.SIMPLIFIED, ChineseScriptConverter.scriptOf("zh-Hans"))
        assertEquals(ChineseScript.SIMPLIFIED, ChineseScriptConverter.scriptOf("zh"))
        assertEquals(null, ChineseScriptConverter.scriptOf("ja"))
        assertEquals(null, ChineseScriptConverter.scriptOf("auto"))
    }

    private fun block(text: String, x: Float, y: Float, width: Float, height: Float): TranslatedTextBlock {
        return TranslatedTextBlock(
            original = text,
            translated = text,
            x = x,
            y = y,
            width = width,
            height = height,
        )
    }
}
