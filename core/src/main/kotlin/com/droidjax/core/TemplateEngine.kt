package com.droidjax.core

object TemplateEngine {
    const val INITIAL_PLACEHOLDER = "<|>"
    const val PLACEHOLDER = "<>"

    fun toInsertOperation(template: Template): InsertOperation {
        val cleanText = StringBuilder()
        val placeholderRanges = mutableListOf<IntRange>()
        var initialCursorPosition: Int? = null
        var index = 0

        while (index < template.body.length) {
            when {
                template.body.startsWith(INITIAL_PLACEHOLDER, index) -> {
                    val position = cleanText.length
                    placeholderRanges += position..position
                    if (initialCursorPosition == null) {
                        initialCursorPosition = position
                    }
                    index += INITIAL_PLACEHOLDER.length
                }

                template.body.startsWith(PLACEHOLDER, index) -> {
                    val position = cleanText.length
                    placeholderRanges += position..position
                    index += PLACEHOLDER.length
                }

                else -> {
                    cleanText.append(template.body[index])
                    index += 1
                }
            }
        }

        val text = cleanText.toString()
        val cursorPosition = initialCursorPosition
            ?: placeholderRanges.firstOrNull()?.first
            ?: text.length

        return InsertOperation(
            text = text,
            cursorOffsetFromEnd = text.length - cursorPosition,
            placeholderRanges = placeholderRanges,
            id = template.id,
            title = template.title,
            category = template.category,
        )
    }
}
