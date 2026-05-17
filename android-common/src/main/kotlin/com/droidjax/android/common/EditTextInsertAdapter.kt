package com.droidjax.android.common

import android.widget.EditText
import com.droidjax.core.InsertOperation
import com.droidjax.core.PlaceholderSession

object EditTextInsertAdapter {
    fun insert(
        editText: EditText,
        operation: InsertOperation,
    ): Int {
        val selectionStart = editText.selectionStart.coerceAtLeast(0)
        val selectionEnd = editText.selectionEnd.coerceAtLeast(0)
        val replaceStart = minOf(selectionStart, selectionEnd)
        val replaceEnd = maxOf(selectionStart, selectionEnd)

        editText.text.replace(replaceStart, replaceEnd, operation.text)

        val session = PlaceholderSession.start(operation)
        val newSelectionStart = replaceStart + session.cursorPosition
        val newSelectionEnd = replaceStart + session.selectionEnd
        editText.setSelection(
            newSelectionStart.coerceIn(0, editText.text.length),
            newSelectionEnd.coerceIn(0, editText.text.length),
        )

        return replaceStart
    }
}
