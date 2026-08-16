package eu.kanade.tachiyomi.data.translation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import eu.kanade.tachiyomi.data.translation.translator.OnDeviceTranslator
import eu.kanade.tachiyomi.data.translation.translator.PageTranslator
import eu.kanade.tachiyomi.data.translation.translator.VisionLlmTranslator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import logcat.LogPriority
import okio.Buffer
import okio.BufferedSource
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.core.common.util.system.logcat
import tachiyomi.decoder.ImageDecoder
import java.io.ByteArrayInputStream
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class PageTranslationService(
    private val preferences: TranslationPreferences,
    private val cache: TranslationCache,
    private val json: Json,
) {
    private val onDeviceTranslator = OnDeviceTranslator()
    private val cloudSemaphore = Semaphore(2)
    private val deviceSemaphore = Semaphore(1)
    private val inFlightMutex = Mutex()
    private val inFlight = mutableMapOf<String, Deferred<ByteArray>>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun translatePage(imageSource: BufferedSource, sourceLangHint: String?): BufferedSource {
        if (!preferences.enabled.get()) return imageSource
        if (ImageUtil.isAnimatedAndSupported(imageSource)) return imageSource

        val originalBytes = imageSource.peek().readByteArray()
        val provider = preferences.provider.get()
        val sourceLang = resolveSourceLang(sourceLangHint)
        val targetLang = preferences.targetLanguage.get().code
        if (isSameLanguage(sourceLang, targetLang)) return imageSource

        val model = if (provider.needsApiKey) {
            preferences.modelPreference(provider).get().ifBlank { provider.defaultModel }
        } else {
            "mlkit"
        }
        val cacheKey = TranslationCache.key(originalBytes, provider, model, sourceLang, targetLang)
        cache.get(cacheKey)?.let { return Buffer().write(it) }

        val deferred = inFlightMutex.withLock {
            inFlight.getOrPut(cacheKey) {
                scope.async {
                    renderTranslated(originalBytes, provider, sourceLang, targetLang)
                        .also { translated ->
                            if (translated !== originalBytes) {
                                cache.put(cacheKey, translated)
                            }
                        }
                }
            }
        }
        return try {
            Buffer().write(deferred.await())
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Page translation failed" }
            imageSource
        } finally {
            inFlightMutex.withLock { inFlight.remove(cacheKey) }
        }
    }

    @Suppress("MagicNumber")
    private suspend fun renderTranslated(
        originalBytes: ByteArray,
        provider: TranslationProvider,
        sourceLang: String,
        targetLang: String,
    ): ByteArray {
        val original = decodeBitmap(originalBytes)
            ?: throw TranslationException("Could not decode page image")
        val prepared = downscaleForProvider(original, provider)
        val blocks = semaphoreFor(provider).withPermit {
            detectAndTranslate(prepared, provider, sourceLang, targetLang)
        }
        if (blocks.isEmpty()) {
            if (prepared !== original) prepared.recycle()
            return originalBytes
        }
        val composed = TranslationCompositor.draw(original, blocks)
        val output = Buffer()
        composed.compress(Bitmap.CompressFormat.JPEG, 90, output.outputStream())
        if (composed !== original) composed.recycle()
        if (prepared !== original) prepared.recycle()
        return output.readByteArray()
    }

    private suspend fun detectAndTranslate(
        bitmap: Bitmap,
        provider: TranslationProvider,
        sourceLang: String,
        targetLang: String,
    ): List<TranslatedTextBlock> {
        val translator = translatorFor(provider)
        val tiles = tilesFor(bitmap, provider)
        return try {
            if (tiles.size == 1) {
                translator.translate(tiles.first().bitmap, sourceLang, targetLang)
            } else if (provider == TranslationProvider.ON_DEVICE) {
                coroutineScope {
                    tiles.map { tile ->
                        async {
                            translator.translate(tile.bitmap, sourceLang, targetLang).map { block ->
                                remapTileBlock(block, tile, bitmap.width, bitmap.height)
                            }
                        }
                    }.awaitAll().flatten()
                }
            } else {
                tiles.flatMap { tile ->
                    translator.translate(tile.bitmap, sourceLang, targetLang).map { block ->
                        remapTileBlock(block, tile, bitmap.width, bitmap.height)
                    }
                }
            }
        } finally {
            tiles.forEach { tile ->
                if (tile.bitmap !== bitmap) tile.bitmap.recycle()
            }
        }
    }

    private fun translatorFor(provider: TranslationProvider): PageTranslator {
        if (provider == TranslationProvider.ON_DEVICE) return onDeviceTranslator
        return VisionLlmTranslator(
            provider = provider,
            apiKey = preferences.apiKeyPreference(provider).get().trim(),
            model = preferences.modelPreference(provider).get().ifBlank { provider.defaultModel },
            json = json,
        )
    }

    private fun semaphoreFor(provider: TranslationProvider): Semaphore {
        return if (provider == TranslationProvider.ON_DEVICE) deviceSemaphore else cloudSemaphore
    }

    @Suppress("MagicNumber")
    private fun tilesFor(bitmap: Bitmap, provider: TranslationProvider): List<ImageTile> {
        val maxTileHeight = if (provider == TranslationProvider.ON_DEVICE) {
            ON_DEVICE_TILE_HEIGHT
        } else {
            CLOUD_TILE_HEIGHT
        }
        if (bitmap.height <= maxTileHeight) {
            return listOf(ImageTile(bitmap, 0))
        }
        val maxTiles = if (provider == TranslationProvider.ON_DEVICE) 12 else 8
        val tileHeight = max(maxTileHeight, ceil(bitmap.height / maxTiles.toFloat()).toInt())
        val overlap = (tileHeight * 0.08f).roundToInt()
        val tiles = mutableListOf<ImageTile>()
        var y = 0
        while (y < bitmap.height) {
            val height = min(tileHeight, bitmap.height - y)
            val tileBitmap = Bitmap.createBitmap(bitmap, 0, y, bitmap.width, height)
            tiles += ImageTile(tileBitmap, y)
            if (y + height >= bitmap.height) break
            y += tileHeight - overlap
        }
        return tiles
    }

    private fun remapTileBlock(
        block: TranslatedTextBlock,
        tile: ImageTile,
        imageWidth: Int,
        imageHeight: Int,
    ): TranslatedTextBlock {
        val pixelX = block.x * tile.bitmap.width
        val pixelY = tile.originY + block.y * tile.bitmap.height
        val pixelW = block.width * tile.bitmap.width
        val pixelH = block.height * tile.bitmap.height
        return block.copy(
            x = pixelX / imageWidth,
            y = pixelY / imageHeight,
            width = pixelW / imageWidth,
            height = pixelH / imageHeight,
        )
    }

    @Suppress("MagicNumber")
    private fun downscaleForProvider(bitmap: Bitmap, provider: TranslationProvider): Bitmap {
        val maxWidth = if (provider == TranslationProvider.ON_DEVICE) {
            ON_DEVICE_MAX_WIDTH
        } else {
            CLOUD_MAX_WIDTH
        }
        if (bitmap.width <= maxWidth) return bitmap
        val ratio = maxWidth / bitmap.width.toFloat()
        val height = max(1, (bitmap.height * ratio).roundToInt())
        return Bitmap.createScaledBitmap(bitmap, maxWidth, height, true)
    }

    private fun decodeBitmap(bytes: ByteArray): Bitmap? {
        return try {
            ImageDecoder.newInstance(ByteArrayInputStream(bytes))?.decode()
                ?: BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }

    private fun resolveSourceLang(sourceLangHint: String?): String {
        val selected = preferences.sourceLanguage.get()
        if (selected != TranslationLanguage.AUTO) return selected.code
        return normalizeLanguageCode(sourceLangHint)
    }

    companion object {
        private const val CLOUD_MAX_WIDTH = 1280
        private const val ON_DEVICE_MAX_WIDTH = 1600
        private const val CLOUD_TILE_HEIGHT = 1600
        private const val ON_DEVICE_TILE_HEIGHT = 2400

        fun normalizeLanguageCode(raw: String?): String {
            val hint = raw.orEmpty().trim().lowercase().replace('_', '-')
            if (hint.isEmpty() || hint == "auto" || hint == "all" || hint == "other") return "auto"
            return when {
                hint == "ja" || hint == "jp" || hint.startsWith("ja-") -> "ja"
                hint == "ko" || hint == "kr" || hint.startsWith("ko-") -> "ko"
                hint == "zh-hant" ||
                    hint == "zh-tw" ||
                    hint == "zh-hk" ||
                    hint == "zh-mo" ||
                    hint.startsWith("zh-hant") -> "zh-Hant"
                hint == "zh" ||
                    hint == "cn" ||
                    hint == "chi" ||
                    hint == "zho" ||
                    hint == "zh-hans" ||
                    hint == "zh-cn" ||
                    hint == "zh-sg" ||
                    hint.startsWith("zh-hans") ||
                    hint.startsWith("zh-") -> "zh-Hans"
                else -> hint.substringBefore('-')
            }
        }

        fun isSameLanguage(sourceLang: String, targetLang: String): Boolean {
            if (sourceLang == "auto" || targetLang == "auto") return false
            return normalizeLanguageCode(sourceLang) == normalizeLanguageCode(targetLang)
        }
    }

    private data class ImageTile(
        val bitmap: Bitmap,
        val originY: Int,
    )
}
