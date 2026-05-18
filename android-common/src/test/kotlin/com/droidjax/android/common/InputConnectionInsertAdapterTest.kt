package com.droidjax.android.common

import android.view.View
import android.view.inputmethod.BaseInputConnection
import com.droidjax.core.InsertOperation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class InputConnectionInsertAdapterTest {
    @Test
    fun `cursor offset maps to InputConnection new cursor position`() {
        val operation = InsertOperation(
            text = "\\frac{}{}",
            cursorOffsetFromEnd = 3,
        )

        val newCursorPosition = with(InputConnectionInsertAdapter) {
            operation.toInputConnectionCursorPosition()
        }

        assertEquals(-2, newCursorPosition)
    }

    @Test
    fun `commit wraps operation in batch edit and passes clean text`() {
        val inputConnection = RecordingInputConnection()
        val operation = InsertOperation(
            text = "\\sqrt{}",
            cursorOffsetFromEnd = 1,
        )

        val committed = InputConnectionInsertAdapter.commit(inputConnection, operation)

        assertTrue(committed)
        assertEquals(1, inputConnection.beginBatchEditCount)
        assertEquals(1, inputConnection.endBatchEditCount)
        assertEquals("\\sqrt{}", inputConnection.committedText.toString())
        assertEquals(0, inputConnection.newCursorPosition)
    }

    @Test
    fun `commit ends batch edit when commitText fails`() {
        val inputConnection = RecordingInputConnection(commitResult = false)
        val operation = InsertOperation(
            text = "\\alpha",
            cursorOffsetFromEnd = 0,
        )

        val committed = InputConnectionInsertAdapter.commit(inputConnection, operation)

        assertFalse(committed)
        assertEquals(1, inputConnection.beginBatchEditCount)
        assertEquals(1, inputConnection.endBatchEditCount)
    }

    private class RecordingInputConnection(
        private val commitResult: Boolean = true,
    ) : BaseInputConnection(
        View(RuntimeEnvironment.getApplication()),
        true,
    ) {
        var beginBatchEditCount: Int = 0
            private set
        var endBatchEditCount: Int = 0
            private set
        var committedText: CharSequence = ""
            private set
        var newCursorPosition: Int? = null
            private set

        override fun beginBatchEdit(): Boolean {
            beginBatchEditCount += 1
            return true
        }

        override fun commitText(
            text: CharSequence,
            newCursorPosition: Int,
        ): Boolean {
            committedText = text
            this.newCursorPosition = newCursorPosition
            return commitResult
        }

        override fun endBatchEdit(): Boolean {
            endBatchEditCount += 1
            return true
        }
    }
}
