package com.droidjax.core

object PlaceholderNavigator {
    fun nextPlaceholderTarget(
        operation: InsertOperation,
        currentCursorPosition: Int,
    ): Placeholder? = operation.placeholders.firstOrNull { it.start > currentCursorPosition }

    fun nextPlaceholder(
        operation: InsertOperation,
        currentCursorPosition: Int,
    ): IntRange? = nextPlaceholderTarget(operation, currentCursorPosition)?.range

    fun nextCursorPosition(
        operation: InsertOperation,
        currentCursorPosition: Int,
    ): Int = nextPlaceholderTarget(operation, currentCursorPosition)?.start ?: operation.text.length
}
