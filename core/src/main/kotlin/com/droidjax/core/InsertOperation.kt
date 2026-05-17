package com.droidjax.core

/**
 * Platform-neutral instruction for inserting TeX into a text field.
 *
 * Placeholder ranges are zero-width cursor targets when start == end.
 */
data class InsertOperation(
    val text: String,
    val cursorOffsetFromEnd: Int,
    val placeholderRanges: List<IntRange> = emptyList(),
    val id: String? = null,
    val title: String? = null,
    val category: String? = null,
) {
    init {
        require(cursorOffsetFromEnd >= 0) { "cursorOffsetFromEnd must be non-negative." }
        require(cursorOffsetFromEnd <= text.length) {
            "cursorOffsetFromEnd must not exceed inserted text length."
        }
        placeholderRanges.forEach { range ->
            require(range.first in 0..text.length) {
                "Placeholder start must be within inserted text."
            }
            require(range.last in 0..text.length) {
                "Placeholder end must be within inserted text."
            }
        }
    }

    val initialCursorPosition: Int
        get() = text.length - cursorOffsetFromEnd
}
