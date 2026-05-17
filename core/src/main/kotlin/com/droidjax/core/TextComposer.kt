package com.droidjax.core

data class TextComposer(
    val text: String = "",
    val selectionStart: Int = text.length,
    val selectionEnd: Int = selectionStart,
    val activeSnippetStart: Int? = null,
    val activeSession: PlaceholderSession? = null,
) {
    init {
        require(selectionStart in 0..text.length) { "selectionStart must be within text." }
        require(selectionEnd in 0..text.length) { "selectionEnd must be within text." }
    }

    val cursorPosition: Int
        get() = selectionEnd

    fun insert(operation: InsertOperation): TextComposer {
        val replaceStart = minOf(selectionStart, selectionEnd)
        val replaceEnd = maxOf(selectionStart, selectionEnd)
        val newText = buildString {
            append(text.substring(0, replaceStart))
            append(operation.text)
            append(text.substring(replaceEnd))
        }
        val session = PlaceholderSession.start(operation)
        val newSelectionStart = replaceStart + session.cursorPosition
        val newSelectionEnd = replaceStart + session.selectionEnd

        return copy(
            text = newText,
            selectionStart = newSelectionStart,
            selectionEnd = newSelectionEnd,
            activeSnippetStart = replaceStart,
            activeSession = session,
        )
    }

    fun insert(snippet: Snippet): TextComposer =
        insert(snippet.toInsertOperation())

    fun moveSelection(
        start: Int,
        end: Int = start,
    ): TextComposer =
        copy(
            selectionStart = start.coerceIn(0, text.length),
            selectionEnd = end.coerceIn(0, text.length),
            activeSnippetStart = null,
            activeSession = null,
        )

    fun nextPlaceholder(): TextComposer {
        val session = activeSession ?: return this
        val snippetStart = activeSnippetStart ?: return this
        val nextSession = session.next()

        return copy(
            selectionStart = snippetStart + nextSession.cursorPosition,
            selectionEnd = snippetStart + nextSession.selectionEnd,
            activeSession = nextSession,
        )
    }
}
