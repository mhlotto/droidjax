package com.droidjax.core

data class PlaceholderSession(
    val operation: InsertOperation,
    val currentPlaceholderIndex: Int = operation.initialPlaceholderIndex(),
) {
    init {
        require(currentPlaceholderIndex in 0..operation.placeholders.size) {
            "Current placeholder index must point at a placeholder or the final cursor position."
        }
    }

    val currentPlaceholder: Placeholder?
        get() = operation.placeholders.getOrNull(currentPlaceholderIndex)

    val cursorPosition: Int
        get() = currentPlaceholder?.start ?: operation.text.length

    val selectionEnd: Int
        get() = currentPlaceholder?.end ?: operation.text.length

    val isComplete: Boolean
        get() = currentPlaceholderIndex >= operation.placeholders.size

    fun next(): PlaceholderSession =
        copy(
            currentPlaceholderIndex = (currentPlaceholderIndex + 1)
                .coerceAtMost(operation.placeholders.size),
        )

    companion object {
        fun start(operation: InsertOperation): PlaceholderSession =
            PlaceholderSession(operation)
    }
}

private fun InsertOperation.initialPlaceholderIndex(): Int =
    placeholders.indexOfFirst { placeholder ->
        placeholder.start == initialCursorPosition
    }.let { index ->
        if (index == -1) placeholders.size else index
    }
