package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.data.translation.TranslationCache
import eu.kanade.tachiyomi.data.translation.TranslationLanguage
import eu.kanade.tachiyomi.data.translation.TranslationPreferences
import eu.kanade.tachiyomi.data.translation.TranslationProvider
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
private fun TranslationLanguage.label(): String {
    return when (this) {
        TranslationLanguage.AUTO -> stringResource(TLMR.strings.translation_lang_auto)
        TranslationLanguage.CHINESE_SIMPLIFIED ->
            stringResource(TLMR.strings.translation_lang_chinese_simplified)
        TranslationLanguage.CHINESE_TRADITIONAL ->
            stringResource(TLMR.strings.translation_lang_chinese_traditional)
        else -> displayName()
    }
}

object SettingsTranslationScreen : SearchableSettings {

    private fun readResolve(): Any = SettingsTranslationScreen

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = TLMR.strings.pref_category_translation

    @Composable
    override fun getPreferences(): List<Preference> {
        val prefs = remember { Injekt.get<TranslationPreferences>() }
        val cache = remember { Injekt.get<TranslationCache>() }
        val provider by prefs.provider.collectAsState()
        return listOf(
            getGeneralGroup(prefs, provider),
            getProviderGroup(prefs, provider),
            getCacheGroup(cache),
        )
    }

    @Composable
    private fun getGeneralGroup(
        prefs: TranslationPreferences,
        provider: TranslationProvider,
    ): Preference.PreferenceGroup {
        val context = LocalContext.current
        return Preference.PreferenceGroup(
            title = stringResource(TLMR.strings.pref_translation_group_general),
            preferenceItems = listOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = prefs.enabled,
                    title = stringResource(TLMR.strings.pref_translation_enabled),
                    subtitle = stringResource(TLMR.strings.pref_translation_enabled_summary),
                    onValueChanged = { enabled ->
                        if (enabled && !prefs.hasCredentials(provider)) {
                            context.toast(TLMR.strings.translation_missing_api_key)
                            false
                        } else {
                            true
                        }
                    },
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = prefs.provider,
                    entries = TranslationProvider.entries.associateWith { stringResource(it.titleRes) },
                    title = stringResource(TLMR.strings.pref_translation_provider),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = prefs.sourceLanguage,
                    entries = TranslationLanguage.sourceLanguages.associateWith { it.label() },
                    title = stringResource(TLMR.strings.pref_translation_source_language),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = prefs.targetLanguage,
                    entries = TranslationLanguage.targetLanguages.associateWith { it.label() },
                    title = stringResource(TLMR.strings.pref_translation_target_language),
                ),
                Preference.PreferenceItem.InfoPreference(
                    title = if (provider == TranslationProvider.ON_DEVICE) {
                        stringResource(TLMR.strings.translation_info_on_device)
                    } else {
                        stringResource(TLMR.strings.translation_info)
                    },
                ),
            ),
        )
    }

    @Composable
    private fun getProviderGroup(
        prefs: TranslationPreferences,
        provider: TranslationProvider,
    ): Preference.PreferenceGroup {
        val uriHandler = LocalUriHandler.current
        val apiKey by prefs.apiKeyPreference(provider).collectAsState()
        val items = buildList {
            if (provider.needsApiKey) {
                add(
                    Preference.PreferenceItem.EditTextInfoPreference(
                        preference = prefs.apiKeyPreference(provider),
                        dialogSubtitle = stringResource(TLMR.strings.pref_translation_api_key_help),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        title = stringResource(TLMR.strings.pref_translation_api_key),
                        subtitle = if (apiKey.isBlank()) {
                            stringResource(TLMR.strings.pref_translation_api_key_not_set)
                        } else {
                            stringResource(TLMR.strings.pref_translation_api_key_set, apiKey.takeLast(4))
                        },
                    ),
                )
                add(
                    Preference.PreferenceItem.EditTextPreference(
                        preference = prefs.modelPreference(provider),
                        title = stringResource(TLMR.strings.pref_translation_model),
                    ),
                )
                add(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(TLMR.strings.pref_translation_get_api_key),
                        subtitle = provider.apiKeyUrl,
                        onClick = { uriHandler.openUri(provider.apiKeyUrl) },
                    ),
                )
            } else {
                add(
                    Preference.PreferenceItem.InfoPreference(
                        title = stringResource(TLMR.strings.translation_info_on_device_models),
                    ),
                )
            }
        }
        return Preference.PreferenceGroup(
            title = stringResource(provider.titleRes),
            preferenceItems = items,
        )
    }

    @Composable
    private fun getCacheGroup(cache: TranslationCache): Preference.PreferenceGroup {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var cacheSize by remember { mutableStateOf(cache.readableSize) }
        return Preference.PreferenceGroup(
            title = stringResource(TLMR.strings.pref_translation_group_cache),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(TLMR.strings.pref_translation_clear_cache),
                    subtitle = stringResource(TLMR.strings.pref_translation_clear_cache_summary, cacheSize),
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) { cache.clear() }
                            cacheSize = cache.readableSize
                            context.toast(TLMR.strings.pref_translation_cache_cleared)
                        }
                    },
                ),
            ),
        )
    }
}
