package eu.kanade.tachiyomi.data.translation.translator

import android.graphics.Bitmap
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import eu.kanade.tachiyomi.data.translation.TranslatedTextBlock
import eu.kanade.tachiyomi.data.translation.TranslationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

class OnDeviceTranslator : PageTranslator {

    private val recognizers = mutableMapOf<String, TextRecognizer>()
    private val translators = mutableMapOf<String, Translator>()
    private val mutex = Mutex()

    override suspend fun translate(
        bitmap: Bitmap,
        sourceLang: String,
        targetLang: String,
    ): List<TranslatedTextBlock> {
        val ocr = recognize(bitmap, sourceLang)
        if (ocr.blocks.isEmpty()) return emptyList()

        val from = mlKitLanguage(ocr.lang)
            ?: throw TranslationException("On-device translation does not support $sourceLang")
        val to = mlKitLanguage(targetLang)
            ?: throw TranslationException("On-device translation does not support $targetLang")

        if (from == to) {
            return ocr.blocks
        }

        val translator = mutex.withLock {
            translators.getOrPut("$from-$to") {
                Translation.getClient(
                    TranslatorOptions.Builder()
                        .setSourceLanguage(from)
                        .setTargetLanguage(to)
                        .build(),
                )
            }
        }
        try {
            translator.downloadModelIfNeeded().await()
        } catch (e: Exception) {
            throw TranslationException(
                "Could not download on-device language models. Check your connection.",
                e,
            )
        }

        return ocr.blocks.map { block ->
            val translated = runCatching {
                translator.translate(block.original).await()
            }.getOrDefault(block.original)
            block.copy(translated = translated)
        }
    }

    private suspend fun recognize(bitmap: Bitmap, sourceLang: String): OcrPage {
        val scripts = scriptsFor(sourceLang)
        val image = InputImage.fromBitmap(bitmap, 0)
        var best = OcrPage(sourceLang.takeIf { it != "auto" } ?: "ja", emptyList())
        var bestScore = -1
        for (script in scripts) {
            val recognizer = mutex.withLock {
                recognizers.getOrPut(script) { createRecognizer(script) }
            }
            val result = runCatching { recognizer.process(image).await() }.getOrNull() ?: continue
            val blocks = result.textBlocks.mapNotNull { it.toBlock(bitmap.width, bitmap.height) }
            val score = blocks.sumOf { it.original.length }
            if (score > bestScore) {
                bestScore = score
                best = OcrPage(langFromScript(script, sourceLang), blocks)
            }
        }
        return best
    }

    private fun Text.TextBlock.toBlock(imageWidth: Int, imageHeight: Int): TranslatedTextBlock? {
        val box = boundingBox ?: return null
        val text = text.trim()
        if (text.isBlank()) return null
        return TranslatedTextBlock(
            original = text,
            translated = text,
            x = box.left.toFloat() / imageWidth,
            y = box.top.toFloat() / imageHeight,
            width = box.width().toFloat() / imageWidth,
            height = box.height().toFloat() / imageHeight,
        )
    }

    private fun createRecognizer(script: String): TextRecognizer {
        return when (script) {
            SCRIPT_JA -> TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
            SCRIPT_KO -> TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
            SCRIPT_ZH -> TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
            else -> TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        }
    }

    private fun scriptsFor(sourceLang: String): List<String> {
        return when {
            sourceLang == "ja" -> listOf(SCRIPT_JA)
            sourceLang == "ko" -> listOf(SCRIPT_KO)
            sourceLang.startsWith("zh") -> listOf(SCRIPT_ZH)
            sourceLang == "auto" -> listOf(SCRIPT_ZH, SCRIPT_JA, SCRIPT_KO, SCRIPT_LA)
            else -> listOf(SCRIPT_LA, SCRIPT_ZH, SCRIPT_JA, SCRIPT_KO)
        }
    }

    private fun langFromScript(script: String, fallback: String): String {
        return when (script) {
            SCRIPT_JA -> "ja"
            SCRIPT_KO -> "ko"
            SCRIPT_ZH -> if (fallback.startsWith("zh")) fallback else "zh-Hans"
            else -> fallback.takeIf { it != "auto" } ?: "en"
        }
    }

    private fun mlKitLanguage(code: String): String? {
        return when {
            code.startsWith("zh") -> TranslateLanguage.CHINESE
            else -> TranslateLanguage.fromLanguageTag(code.substringBefore('-'))
        }
    }

    private data class OcrPage(
        val lang: String,
        val blocks: List<TranslatedTextBlock>,
    )

    companion object {
        private const val SCRIPT_JA = "ja"
        private const val SCRIPT_KO = "ko"
        private const val SCRIPT_ZH = "zh"
        private const val SCRIPT_LA = "la"
    }
}
