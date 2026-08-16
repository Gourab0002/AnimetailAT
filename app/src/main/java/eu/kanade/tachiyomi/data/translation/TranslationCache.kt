package eu.kanade.tachiyomi.data.translation

import android.content.Context
import android.text.format.Formatter
import com.jakewharton.disklrucache.DiskLruCache
import eu.kanade.tachiyomi.util.storage.DiskUtil
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.File
import java.security.MessageDigest

class TranslationCache(context: Context) {

    private val appContext = context.applicationContext

    private val diskCache = DiskLruCache.open(
        File(appContext.cacheDir, "page_translation_cache"),
        CACHE_VERSION,
        1,
        CACHE_SIZE,
    )

    val readableSize: String
        get() = Formatter.formatFileSize(appContext, DiskUtil.getDirectorySize(diskCache.directory))

    fun get(key: String): ByteArray? {
        return try {
            diskCache.get(key)?.use { snapshot ->
                snapshot.getInputStream(0).use { it.readBytes() }
            }
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) { "Failed to read translation cache" }
            null
        }
    }

    fun put(key: String, bytes: ByteArray) {
        var editor: DiskLruCache.Editor? = null
        try {
            editor = diskCache.edit(key) ?: return
            editor.newOutputStream(0).use { it.write(bytes) }
            diskCache.flush()
            editor.commit()
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) { "Failed to write translation cache" }
        } finally {
            editor?.abortUnlessCommitted()
        }
    }

    fun clear() {
        diskCache.directory.listFiles()?.forEach { file ->
            val name = file.name
            if (name == "journal" || name.startsWith("journal.")) return@forEach
            try {
                diskCache.remove(name.substringBeforeLast("."))
            } catch (e: Exception) {
                logcat(LogPriority.WARN, e) { "Failed to remove translation cache entry" }
            }
        }
    }

    companion object {
        const val COMPOSITOR_VERSION = 1
        private const val CACHE_VERSION = 1
        private const val CACHE_SIZE = 150L * 1024 * 1024

        fun key(
            imageBytes: ByteArray,
            provider: TranslationProvider,
            model: String,
            sourceLang: String,
            targetLang: String,
        ): String {
            val digest = MessageDigest.getInstance("MD5")
            digest.update(imageBytes)
            digest.update(provider.name.toByteArray())
            digest.update(model.toByteArray())
            digest.update(sourceLang.toByteArray())
            digest.update(targetLang.toByteArray())
            digest.update(COMPOSITOR_VERSION.toString().toByteArray())
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
}
