package eu.kanade.tachiyomi.data.translation

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum

class TranslationPreferences(private val preferenceStore: PreferenceStore) {

    val enabled = preferenceStore.getBoolean("page_translation_enabled", false)

    val provider = preferenceStore.getEnum("page_translation_provider", TranslationProvider.XAI)

    val sourceLanguage = preferenceStore.getEnum(
        "page_translation_source_lang",
        TranslationLanguage.AUTO,
    )

    val targetLanguage = preferenceStore.getEnum(
        "page_translation_target_lang",
        TranslationLanguage.ENGLISH,
    )

    val xaiApiKey = preferenceStore.getString("page_translation_xai_api_key", "")

    val openRouterApiKey = preferenceStore.getString("page_translation_openrouter_api_key", "")

    val geminiApiKey = preferenceStore.getString("page_translation_gemini_api_key", "")

    val openaiApiKey = preferenceStore.getString("page_translation_openai_api_key", "")

    val xaiModel = preferenceStore.getString("page_translation_xai_model", TranslationProvider.XAI.defaultModel)

    val openRouterModel = preferenceStore.getString(
        "page_translation_openrouter_model",
        TranslationProvider.OPENROUTER.defaultModel,
    )

    val geminiModel = preferenceStore.getString(
        "page_translation_gemini_model",
        TranslationProvider.GEMINI.defaultModel,
    )

    val openaiModel = preferenceStore.getString(
        "page_translation_openai_model",
        TranslationProvider.OPENAI.defaultModel,
    )

    fun apiKeyPreference(provider: TranslationProvider = this.provider.get()): Preference<String> {
        return when (provider) {
            TranslationProvider.XAI -> xaiApiKey
            TranslationProvider.OPENROUTER -> openRouterApiKey
            TranslationProvider.GEMINI -> geminiApiKey
            TranslationProvider.OPENAI -> openaiApiKey
            TranslationProvider.ON_DEVICE -> xaiApiKey
        }
    }

    fun modelPreference(provider: TranslationProvider = this.provider.get()): Preference<String> {
        return when (provider) {
            TranslationProvider.XAI -> xaiModel
            TranslationProvider.OPENROUTER -> openRouterModel
            TranslationProvider.GEMINI -> geminiModel
            TranslationProvider.OPENAI -> openaiModel
            TranslationProvider.ON_DEVICE -> xaiModel
        }
    }

    fun hasCredentials(provider: TranslationProvider = this.provider.get()): Boolean {
        return !provider.needsApiKey || apiKeyPreference(provider).get().isNotBlank()
    }
}
