package eu.kanade.tachiyomi.data.translation

import kotlin.math.max
import kotlin.math.min

/**
 * Drops text blocks that repeat the same bubble across overlapping page tiles.
 * The kept block is the one with the longer translation.
 */
internal fun dedupeOverlappingBlocks(blocks: List<TranslatedTextBlock>): List<TranslatedTextBlock> {
    if (blocks.size < 2) return blocks
    val ordered = blocks.sortedByDescending { it.translated.length }
    val kept = mutableListOf<TranslatedTextBlock>()
    for (block in ordered) {
        val overlapsKept = kept.any { existing ->
            overlapRatio(existing, block) >= DUPLICATE_OVERLAP
        }
        if (!overlapsKept) kept += block
    }
    return kept
}

private fun overlapRatio(first: TranslatedTextBlock, second: TranslatedTextBlock): Float {
    val left = max(first.x, second.x)
    val top = max(first.y, second.y)
    val right = min(first.x + first.width, second.x + second.width)
    val bottom = min(first.y + first.height, second.y + second.height)
    val overlapWidth = right - left
    val overlapHeight = bottom - top
    if (overlapWidth <= 0f || overlapHeight <= 0f) return 0f
    val intersection = overlapWidth * overlapHeight
    val smallerArea = min(first.width * first.height, second.width * second.height)
    if (smallerArea <= 0f) return 0f
    return intersection / smallerArea
}

private const val DUPLICATE_OVERLAP = 0.5f
