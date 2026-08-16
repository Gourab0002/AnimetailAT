package eu.kanade.tachiyomi.data.translation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.graphics.ColorUtils
import kotlin.math.max
import kotlin.math.min

object TranslationCompositor {

    @Suppress("MagicNumber", "LongMethod")
    fun draw(source: Bitmap, blocks: List<TranslatedTextBlock>): Bitmap {
        if (blocks.isEmpty()) return source

        val working = if (source.isMutable) {
            source
        } else {
            source.copy(Bitmap.Config.ARGB_8888, true)
        }
        val canvas = Canvas(working)
        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.LEFT
        }

        for (block in blocks) {
            val text = block.translated.trim().ifBlank { block.original.trim() }
            if (text.isBlank()) continue

            val rect = toRect(block, working.width, working.height) ?: continue
            expand(rect, working.width, working.height)
            if (rect.width() < 8f || rect.height() < 8f) continue

            val fill = sampleFillColor(working, rect)
            val textColor = if (luminance(fill) > 0.55f) Color.BLACK else Color.WHITE
            boxPaint.color = ColorUtils.setAlphaComponent(fill, 230)
            canvas.drawRoundRect(rect, 10f, 10f, boxPaint)

            val rotate = rect.height() > rect.width() * 1.45f && isMostlyLatin(text)
            if (rotate) {
                drawRotatedText(canvas, text, rect, textPaint, textColor)
            } else {
                drawFittedText(canvas, text, rect, textPaint, textColor)
            }
        }
        return working
    }

    @Suppress("MagicNumber")
    private fun toRect(block: TranslatedTextBlock, imageWidth: Int, imageHeight: Int): RectF? {
        val looksLikePixels = block.x > 1.5f ||
            block.y > 1.5f ||
            block.width > 1.5f ||
            block.height > 1.5f
        val left: Float
        val top: Float
        val right: Float
        val bottom: Float
        if (looksLikePixels) {
            left = block.x
            top = block.y
            right = block.x + block.width
            bottom = block.y + block.height
        } else {
            left = block.x * imageWidth
            top = block.y * imageHeight
            right = (block.x + block.width) * imageWidth
            bottom = (block.y + block.height) * imageHeight
        }
        if (right <= left || bottom <= top) return null
        return RectF(
            left.coerceIn(0f, imageWidth.toFloat()),
            top.coerceIn(0f, imageHeight.toFloat()),
            right.coerceIn(0f, imageWidth.toFloat()),
            bottom.coerceIn(0f, imageHeight.toFloat()),
        )
    }

    @Suppress("MagicNumber")
    private fun expand(rect: RectF, imageWidth: Int, imageHeight: Int) {
        val padX = max(4f, rect.width() * 0.08f)
        val padY = max(3f, rect.height() * 0.08f)
        rect.left = max(0f, rect.left - padX)
        rect.top = max(0f, rect.top - padY)
        rect.right = min(imageWidth.toFloat(), rect.right + padX)
        rect.bottom = min(imageHeight.toFloat(), rect.bottom + padY)
    }

    @Suppress("MagicNumber")
    private fun sampleFillColor(bitmap: Bitmap, rect: RectF): Int {
        val samples = listOf(
            rect.left + 2f to rect.top + 2f,
            rect.right - 3f to rect.top + 2f,
            rect.left + 2f to rect.bottom - 3f,
            rect.right - 3f to rect.bottom - 3f,
        )
        var r = 0
        var g = 0
        var b = 0
        var count = 0
        for ((x, y) in samples) {
            val ix = x.toInt().coerceIn(0, bitmap.width - 1)
            val iy = y.toInt().coerceIn(0, bitmap.height - 1)
            val color = bitmap.getPixel(ix, iy)
            r += Color.red(color)
            g += Color.green(color)
            b += Color.blue(color)
            count++
        }
        if (count == 0) return Color.WHITE
        return Color.rgb(r / count, g / count, b / count)
    }

    @Suppress("MagicNumber")
    private fun luminance(color: Int): Float {
        return (0.299f * Color.red(color) + 0.587f * Color.green(color) + 0.114f * Color.blue(color)) / 255f
    }

    @Suppress("MagicNumber")
    private fun drawFittedText(
        canvas: Canvas,
        text: String,
        rect: RectF,
        paint: TextPaint,
        color: Int,
    ) {
        paint.color = color
        val maxWidth = max(8, (rect.width() - 8f).toInt())
        val maxHeight = rect.height() - 6f
        val cjk = text.any { it in '\u3400'..'\u9FFF' || it in '\uF900'..'\uFAFF' }
        var size = min(rect.height() * if (cjk) 0.52f else 0.42f, if (cjk) 44f else 40f)
        var layout: StaticLayout
        while (true) {
            paint.textSize = size
            layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, maxWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
            if (layout.height <= maxHeight || size <= 8f) break
            size -= 1f
        }
        canvas.save()
        canvas.translate(rect.left + 4f, rect.top + max(0f, (rect.height() - layout.height) / 2f))
        layout.draw(canvas)
        canvas.restore()
    }

    @Suppress("MagicNumber")
    private fun drawRotatedText(
        canvas: Canvas,
        text: String,
        rect: RectF,
        paint: TextPaint,
        color: Int,
    ) {
        canvas.save()
        canvas.translate(rect.centerX(), rect.centerY())
        canvas.rotate(90f)
        val rotated = RectF(
            -rect.height() / 2f,
            -rect.width() / 2f,
            rect.height() / 2f,
            rect.width() / 2f,
        )
        drawFittedText(canvas, text, rotated, paint, color)
        canvas.restore()
    }

    private fun isMostlyLatin(text: String): Boolean {
        val letters = text.filter { it.isLetter() }
        if (letters.isEmpty()) return true
        val latin = letters.count { it.code < 0x0300 }
        return latin * 2 >= letters.length
    }
}
