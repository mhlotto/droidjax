package com.droidjax.core

object PlaceholderNavigator {
    fun nextPlaceholder(
        operation: InsertOperation,
        currentCursorPosition: Int,
    ): IntRange? = operation.placeholderRanges.firstOrNull { it.first > currentCursorPosition }

    fun nextCursorPosition(
        operation: InsertOperation,
        currentCursorPosition: Int,
    ): Int = nextPlaceholder(operation, currentCursorPosition)?.first ?: operation.text.length
}
