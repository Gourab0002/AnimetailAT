package eu.kanade.tachiyomi.data.translation

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.tail.TLMR
import java.util.Locale

enum class TranslationProvider(
    val titleRes: StringResource,
    val needsApiKey: Boolean,
    val defaultModel: String,
    val baseUrl: String,
    val apiKeyUrl: String,
) {
    ON_DEVICE(
        titleRes = TLMR.strings.pref_translation_provider_on_device,
        needsApiKey = false,
        defaultModel = "",
        baseUrl = "",
        apiKeyUrl = "",
    ),
    XAI(
        titleRes = TLMR.strings.pref_translation_provider_xai,
        needsApiKey = true,
        defaultModel = "grok-4.6",
        baseUrl = "https://api.x.ai/v1",
        apiKeyUrl = "https://console.x.ai",
    ),
    OPENROUTER(
        titleRes = TLMR.strings.pref_translation_provider_openrouter,
        needsApiKey = true,
        defaultModel = "google/gemini-2.5-flash",
        baseUrl = "https://openrouter.ai/api/v1",
        apiKeyUrl = "https://openrouter.ai/keys",
    ),
    GEMINI(
        titleRes = TLMR.strings.pref_translation_provider_gemini,
        needsApiKey = true,
        defaultModel = "gemini-2.5-flash",
        baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai",
        apiKeyUrl = "https://aistudio.google.com/apikey",
    ),
    OPENAI(
        titleRes = TLMR.strings.pref_translation_provider_openai,
        needsApiKey = true,
        defaultModel = "gpt-4o-mini",
        baseUrl = "https://api.openai.com/v1",
        apiKeyUrl = "https://platform.openai.com/api-keys",
    ),
}

enum class TranslationLanguage(val code: String) {
    AUTO("auto"),
    JAPANESE("ja"),
    KOREAN("ko"),
    CHINESE_SIMPLIFIED("zh-Hans"),
    CHINESE_TRADITIONAL("zh-Hant"),
    ENGLISH("en"),
    SPANISH("es"),
    PORTUGUESE("pt"),
    FRENCH("fr"),
    GERMAN("de"),
    RUSSIAN("ru"),
    INDONESIAN("id"),
    VIETNAMESE("vi"),
    THAI("th"),
    ARABIC("ar"),
    ITALIAN("it"),
    TURKISH("tr"),
    POLISH("pl"),
    UKRAINIAN("uk"),
    ;

    fun displayName(): String {
        return when (this) {
            AUTO -> ""
            CHINESE_SIMPLIFIED -> Locale.forLanguageTag("zh-Hans")
                .getDisplayName(Locale.getDefault())
                .ifBlank { "Chinese (Simplified)" }
            CHINESE_TRADITIONAL -> Locale.forLanguageTag("zh-Hant")
                .getDisplayName(Locale.getDefault())
                .ifBlank { "Chinese (Traditional)" }
            else -> Locale.forLanguageTag(code).getDisplayName(Locale.getDefault()).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        }
    }

    companion object {
        val sourceLanguages = listOf(
            AUTO,
            JAPANESE,
            KOREAN,
            CHINESE_SIMPLIFIED,
            CHINESE_TRADITIONAL,
        )
        val targetLanguages = entries.filter { it != AUTO }
    }
}

data class TranslatedTextBlock(
    val original: String,
    val translated: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
)

class TranslationException(message: String, cause: Throwable? = null) : Exception(message, cause)
