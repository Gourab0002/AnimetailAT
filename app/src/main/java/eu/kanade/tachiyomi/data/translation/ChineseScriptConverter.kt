package eu.kanade.tachiyomi.data.translation

import android.icu.text.Transliterator

internal enum class ChineseScript {
    SIMPLIFIED,
    TRADITIONAL,
}

/**
 * ML Kit translates every Chinese variant as Simplified Chinese.
 * ICU converts the result when the reader asked for the other script.
 */
internal object ChineseScriptConverter {

    fun scriptOf(code: String): ChineseScript? {
        val normalized = code.trim().lowercase().replace('_', '-')
        return when {
            normalized == "zh-hant" ||
                normalized == "zh-tw" ||
                normalized == "zh-hk" ||
                normalized == "zh-mo" ||
                normalized.startsWith("zh-hant") -> ChineseScript.TRADITIONAL
            normalized == "zh" ||
                normalized == "zh-hans" ||
                normalized == "zh-cn" ||
                normalized == "zh-sg" ||
                normalized.startsWith("zh-hans") ||
                normalized.startsWith("zh-") -> ChineseScript.SIMPLIFIED
            else -> null
        }
    }

    fun convert(text: String, target: ChineseScript): String {
        val ids = when (target) {
            ChineseScript.TRADITIONAL -> listOf("Hans-Hant", "Simplified-Traditional")
            ChineseScript.SIMPLIFIED -> listOf("Hant-Hans", "Traditional-Simplified")
        }
        for (id in ids) {
            val converted = runCatching { Transliterator.getInstance(id).transliterate(text) }
                .getOrNull()
            if (converted != null) return converted
        }
        return text
    }
}
