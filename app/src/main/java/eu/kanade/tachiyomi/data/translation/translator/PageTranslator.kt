package eu.kanade.tachiyomi.data.translation.translator

import android.graphics.Bitmap
import eu.kanade.tachiyomi.data.translation.TranslatedTextBlock

interface PageTranslator {
    suspend fun translate(
        bitmap: Bitmap,
        sourceLang: String,
        targetLang: String,
    ): List<TranslatedTextBlock>
}
