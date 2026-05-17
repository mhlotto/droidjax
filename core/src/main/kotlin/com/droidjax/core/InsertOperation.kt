package com.droidjax.core

/**
 * Platform-neutral instruction for inserting TeX into a text field.
 *
 * Placeholder ranges are kept for simple callers. Use [placeholders] when the
 * frontend needs labels or default selected text.
 */
data class InsertOperation(
    val text: String,
    val cursorOffsetFromEnd: Int,
    val placeholderRanges: List<IntRange> = emptyList(),
    val placeholders: List<Placeholder> = placeholderRanges.mapIndexed { index, range ->
        Placeholder(
            index = index,
            start = range.first,
            end = if (range.first == range.last) range.first else range.last + 1,
        )
    },
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
        placeholders.forEach { placeholder ->
            require(placeholder.start in 0..text.length) {
                "Placeholder start must be within inserted text."
            }
            require(placeholder.end in 0..text.length) {
                "Placeholder end must be within inserted text."
            }
        }
    }

    val initialCursorPosition: Int
        get() = text.length - cursorOffsetFromEnd
}
