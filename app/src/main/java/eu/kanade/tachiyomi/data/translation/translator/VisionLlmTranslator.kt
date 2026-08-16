package eu.kanade.tachiyomi.data.translation.translator

import android.graphics.Bitmap
import android.util.Base64
import eu.kanade.tachiyomi.data.translation.TranslatedTextBlock
import eu.kanade.tachiyomi.data.translation.TranslationException
import eu.kanade.tachiyomi.data.translation.TranslationProvider
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.await
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import logcat.LogPriority
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.core.common.util.system.logcat
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class VisionLlmTranslator(
    private val provider: TranslationProvider,
    private val apiKey: String,
    private val model: String,
    private val json: Json,
) : PageTranslator {

    private val client = httpClient

    override suspend fun translate(
        bitmap: Bitmap,
        sourceLang: String,
        targetLang: String,
    ): List<TranslatedTextBlock> {
        if (apiKey.isBlank()) {
            throw TranslationException("Missing API key for ${provider.name}")
        }
        val imageB64 = encodeJpeg(bitmap)
        val prompt = buildPrompt(sourceLang, targetLang)
        val payload = ChatCompletionRequest(
            model = model.ifBlank { provider.defaultModel },
            temperature = 0.2,
            maxTokens = 8192,
            messages = listOf(
                ChatMessage(
                    role = "user",
                    content = listOf(
                        ChatContent(
                            type = "image_url",
                            imageUrl = ImageUrl(url = "data:image/jpeg;base64,$imageB64"),
                        ),
                        ChatContent(type = "text", text = prompt),
                    ),
                ),
            ),
        )
        val body = json.encodeToString(ChatCompletionRequest.serializer(), payload)
        val headers = Headers.Builder()
            .add("Authorization", "Bearer $apiKey")
            .add("Content-Type", "application/json")
            .apply {
                if (provider == TranslationProvider.OPENROUTER) {
                    add("HTTP-Referer", "https://github.com/Animetailapp/animetail")
                    add("X-Title", "Animetail")
                }
            }
            .build()
        val request = POST(
            url = "${provider.baseUrl.trimEnd('/')}/chat/completions",
            headers = headers,
            body = body.toRequestBody(JSON_MEDIA_TYPE),
        )
        val response = client.newCall(request).await()
        val raw = response.body.string()
        if (!response.isSuccessful) {
            throw TranslationException("Provider error ${response.code}: ${raw.take(280)}")
        }
        val parsed = runCatching {
            json.decodeFromString(ChatCompletionResponse.serializer(), raw)
        }.getOrElse {
            throw TranslationException("Could not parse provider response", it)
        }
        parsed.error?.message?.let { throw TranslationException(it) }
        val content = parsed.choices.firstOrNull()?.message?.content.orEmpty()
        return parseBlocks(content)
    }

    private fun parseBlocks(content: String): List<TranslatedTextBlock> {
        val jsonText = extractJson(content)
        val result = runCatching {
            json.decodeFromString(LlmTranslationResponse.serializer(), jsonText)
        }.getOrElse { error ->
            logcat(LogPriority.WARN, error) { "Failed to parse translation JSON" }
            throw TranslationException("Model did not return usable translation JSON")
        }
        return result.blocks.mapNotNull { block ->
            val translated = block.translated.trim()
            if (translated.isBlank()) return@mapNotNull null
            TranslatedTextBlock(
                original = block.text,
                translated = translated,
                x = block.x,
                y = block.y,
                width = block.w,
                height = block.h,
            )
        }
    }

    private fun extractJson(raw: String): String {
        val trimmed = raw.trim()
        val fence = FENCE_REGEX.find(trimmed)
        if (fence != null) return fence.groupValues[1].trim()
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        if (start >= 0 && end > start) return trimmed.substring(start, end + 1)
        return trimmed
    }

    private fun buildPrompt(sourceLang: String, targetLang: String): String {
        val sourceLabel = languageName(sourceLang)
        val targetLabel = languageName(targetLang)
        val scriptNote = when (targetLang) {
            "zh-Hans" -> " Write the translation in Simplified Chinese (简体中文)."
            "zh-Hant" -> " Write the translation in Traditional Chinese (繁體中文)."
            else -> ""
        }
        return """
            You are a manga, manhwa, manhua, and comic page translator.
            OCR every speech bubble, caption, sign, and readable story text.
            Ignore watermarks, page numbers, and publisher logos that are not story text.
            Translate from $sourceLabel to $targetLabel.$scriptNote
            Keep meaning, tone, and informal speech. Keep names consistent.
            Return ONLY valid JSON with this shape:
            {"blocks":[{"text":"original","translated":"translated","x":0.12,"y":0.34,"w":0.20,"h":0.08}]}
            Coordinates are normalized 0-1 relative to the image, where x,y is the top-left of the text region.
            If there is no text, return {"blocks":[]}.
        """.trimIndent()
    }

    private fun languageName(code: String): String {
        return when (code) {
            "auto", "" -> "the original language on the page (Japanese, Korean, Simplified Chinese, or Traditional Chinese)"
            "ja" -> "Japanese"
            "ko" -> "Korean"
            "zh", "zh-Hans" -> "Simplified Chinese"
            "zh-Hant" -> "Traditional Chinese"
            "en" -> "English"
            "es" -> "Spanish"
            "pt" -> "Portuguese"
            "fr" -> "French"
            "de" -> "German"
            "ru" -> "Russian"
            "id" -> "Indonesian"
            "vi" -> "Vietnamese"
            "th" -> "Thai"
            "ar" -> "Arabic"
            "it" -> "Italian"
            "tr" -> "Turkish"
            "pl" -> "Polish"
            "uk" -> "Ukrainian"
            else -> code
        }
    }

    @Suppress("MagicNumber")
    private fun encodeJpeg(bitmap: Bitmap): String {
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }

    @Serializable
    private data class ChatCompletionRequest(
        val model: String,
        val messages: List<ChatMessage>,
        val temperature: Double = 0.2,
        @SerialName("max_tokens") val maxTokens: Int = 8192,
    )

    @Serializable
    private data class ChatMessage(
        val role: String,
        val content: List<ChatContent>,
    )

    @Serializable
    private data class ChatContent(
        val type: String,
        val text: String? = null,
        @SerialName("image_url") val imageUrl: ImageUrl? = null,
    )

    @Serializable
    private data class ImageUrl(
        val url: String,
        val detail: String = "high",
    )

    @Serializable
    private data class ChatCompletionResponse(
        val choices: List<ChatChoice> = emptyList(),
        val error: ApiError? = null,
    )

    @Serializable
    private data class ChatChoice(
        val message: ChatResponseMessage? = null,
    )

    @Serializable
    private data class ChatResponseMessage(
        val content: String? = null,
    )

    @Serializable
    private data class ApiError(
        val message: String? = null,
    )

    @Serializable
    private data class LlmTranslationResponse(
        val blocks: List<LlmTextBlock> = emptyList(),
    )

    @Serializable
    private data class LlmTextBlock(
        val text: String = "",
        val translated: String = "",
        val x: Float = 0f,
        val y: Float = 0f,
        val w: Float = 0f,
        val h: Float = 0f,
    )

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val FENCE_REGEX = Regex("```(?:json)?\\s*([\\s\\S]*?)```")
        private const val JPEG_QUALITY = 75
        private val httpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(4, TimeUnit.MINUTES)
            .build()
    }
}
