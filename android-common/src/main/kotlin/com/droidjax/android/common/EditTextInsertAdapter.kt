package com.droidjax.android.common

import android.widget.EditText
import com.droidjax.core.InsertOperation
import com.droidjax.core.TextComposer

object EditTextInsertAdapter {
    fun insert(
        editText: EditText,
        operation: InsertOperation,
    ): Int {
        val selectionStart = editText.selectionStart.coerceAtLeast(0)
        val selectionEnd = editText.selectionEnd.coerceAtLeast(0)
        val replaceStart = minOf(selectionStart, selectionEnd)
        val replaceEnd = maxOf(selectionStart, selectionEnd)
        val composer = TextComposer(
            text = editText.text.toString(),
            selectionStart = replaceStart,
            selectionEnd = replaceEnd,
        ).insert(operation)

        editText.text.replace(0, editText.text.length, composer.text)
        editText.setSelection(
            composer.selectionStart.coerceIn(0, editText.text.length),
            composer.selectionEnd.coerceIn(0, editText.text.length),
        )

        return replaceStart
    }
}
