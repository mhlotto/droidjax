package com.droidjax.core

object TemplateEngine {
    const val INITIAL_PLACEHOLDER = "<|>"
    const val PLACEHOLDER = "<>"

    fun toInsertOperation(template: Template): InsertOperation {
        val cleanText = StringBuilder()
        val placeholders = mutableListOf<Placeholder>()
        var initialCursorPosition: Int? = null
        var index = 0

        while (index < template.body.length) {
            val marker = parseMarker(template.body, index)
            if (marker == null) {
                cleanText.append(template.body[index])
                index += 1
            } else {
                val start = cleanText.length
                cleanText.append(marker.defaultText)
                val end = cleanText.length
                placeholders += Placeholder(
                    index = placeholders.size,
                    start = start,
                    end = end,
                    label = marker.label,
                    defaultText = marker.defaultText,
                )
                if (marker.isInitial && initialCursorPosition == null) {
                    initialCursorPosition = start
                }
                index = marker.endIndex
            }
        }

        val text = cleanText.toString()
        val cursorPosition = initialCursorPosition
            ?: placeholders.firstOrNull()?.start
            ?: text.length

        return InsertOperation(
            text = text,
            cursorOffsetFromEnd = text.length - cursorPosition,
            placeholderRanges = placeholders.map { it.range },
            placeholders = placeholders,
            id = template.id,
            title = template.title,
            category = template.category,
        )
    }

    private fun parseMarker(
        body: String,
        startIndex: Int,
    ): ParsedMarker? {
        if (body[startIndex] != '<') return null

        val closeIndex = body.indexOf('>', startIndex = startIndex + 1)
        if (closeIndex == -1) return null

        val rawContent = body.substring(startIndex + 1, closeIndex)
        val isInitial = rawContent.startsWith("|")
        val content = if (isInitial) rawContent.drop(1) else rawContent
        val defaultSeparatorIndex = content.indexOf('=')
        val label = when {
            defaultSeparatorIndex == -1 -> content
            else -> content.substring(0, defaultSeparatorIndex)
        }.trim().ifEmpty { null }
        val defaultText = when {
            defaultSeparatorIndex == -1 -> ""
            else -> content.substring(defaultSeparatorIndex + 1)
        }

        return ParsedMarker(
            isInitial = isInitial,
            label = label,
            defaultText = defaultText,
            endIndex = closeIndex + 1,
        )
    }

    private data class ParsedMarker(
        val isInitial: Boolean,
        val label: String?,
        val defaultText: String,
        val endIndex: Int,
    )
}
