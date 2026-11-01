package eu.kanade.tachiyomi.data.translation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import eu.kanade.tachiyomi.data.translation.translator.OnDeviceTranslator
import eu.kanade.tachiyomi.data.translation.translator.VisionLlmTranslator
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs the real translators on a device or emulator.
 * Cloud providers read keys from instrumentation arguments:
 * xaiKey, openRouterKey, geminiKey, openaiKey.
 */
@RunWith(AndroidJUnit4::class)
class TranslationDeviceTest {

    @Test(timeout = 300_000)
    fun onDeviceTranslatesJapaneseAndTraditionalChinese() = runBlocking {
        val translator = OnDeviceTranslator()
        val japanese = textBitmap("こんにちは")
        try {
            val translated = translator.translate(japanese, "ja", "en")
            val english = translated.joinToString(" ") { it.translated }
            Log.i(TAG, "on-device ja->en: ${translated.size} blocks [$english]")
            assertTrue("On-device OCR found no Japanese text", translated.isNotEmpty())
            val greeting = english.contains("hello", ignoreCase = true) ||
                english.contains("good afternoon", ignoreCase = true) ||
                english.contains("hi", ignoreCase = true)
            assertTrue("On-device translation was '$english'", greeting)

            val alreadyJapanese = translator.translate(japanese, "ja", "ja")
            Log.i(TAG, "on-device ja->ja blocks: ${alreadyJapanese.size}")
            assertTrue(
                "Same-language page should not be painted over",
                alreadyJapanese.isEmpty(),
            )
        } finally {
            japanese.recycle()
        }

        val simplified = textBitmap("中国")
        try {
            val traditional = translator.translate(simplified, "zh-Hans", "zh-Hant")
            val text = traditional.joinToString(" ") { it.translated }
            Log.i(TAG, "on-device zh-Hans->zh-Hant: [$text]")
            assertTrue("Traditional conversion produced no text", text.isNotBlank())
            assertTrue(
                "Expected a traditional form of 中国, got '$text'",
                text.contains("國") || text.contains("中國"),
            )
        } finally {
            simplified.recycle()
        }
    }

    @Test(timeout = 300_000)
    fun apiProvidersTranslateJapaneseGreeting() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        val json = Json { ignoreUnknownKeys = true }
        val page = textBitmap("こんにちは")
        val providers = listOf(
            TranslationProvider.XAI to args.getString("xaiKey").orEmpty(),
            TranslationProvider.OPENROUTER to args.getString("openRouterKey").orEmpty(),
            TranslationProvider.GEMINI to args.getString("geminiKey").orEmpty(),
            TranslationProvider.OPENAI to args.getString("openaiKey").orEmpty(),
        )
        val results = mutableListOf<String>()
        try {
            for ((provider, key) in providers) {
                if (key.isBlank()) {
                    results += "${provider.name}: SKIP no API key"
                    continue
                }
                val outcome = runCatching {
                    VisionLlmTranslator(
                        provider = provider,
                        apiKey = key,
                        model = provider.defaultModel,
                        json = json,
                    ).translate(page, "ja", "en")
                }
                results += outcome.fold(
                    onSuccess = { blocks ->
                        val text = blocks.joinToString(" ") { it.translated }
                        val ok = text.contains("hello", ignoreCase = true)
                        "${provider.name}: ${if (ok) "OK" else "UNEXPECTED"} [$text]"
                    },
                    onFailure = { error ->
                        "${provider.name}: FAIL ${error.message?.take(180)}"
                    },
                )
            }
        } finally {
            page.recycle()
        }
        results.forEach { Log.i(TAG, it) }
        val failed = results.filter { it.contains("FAIL") || it.contains("UNEXPECTED") }
        assertTrue(failed.joinToString("\n"), failed.isEmpty())
    }

    private fun textBitmap(text: String): Bitmap {
        val bitmap = Bitmap.createBitmap(900, 360, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 140f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText(text, 40f, 230f, paint)
        return bitmap
    }

    private companion object {
        const val TAG = "TranslationDeviceTest"
    }
}
