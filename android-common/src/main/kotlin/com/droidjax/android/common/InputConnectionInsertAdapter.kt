package com.droidjax.android.common

import android.view.inputmethod.InputConnection
import com.droidjax.core.InsertOperation

object InputConnectionInsertAdapter {
    fun commit(
        inputConnection: InputConnection,
        operation: InsertOperation,
    ): Boolean {
        inputConnection.beginBatchEdit()
        return try {
            inputConnection.commitText(
                operation.text,
                operation.toInputConnectionCursorPosition(),
            )
        } finally {
            inputConnection.endBatchEdit()
        }
    }

    fun InsertOperation.toInputConnectionCursorPosition(): Int =
        1 - cursorOffsetFromEnd
}
