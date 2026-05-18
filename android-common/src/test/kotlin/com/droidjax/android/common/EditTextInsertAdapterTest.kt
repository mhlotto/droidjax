package com.droidjax.android.common

import android.widget.EditText
import com.droidjax.core.SnippetCatalog
import com.droidjax.core.Template
import com.droidjax.core.TemplateEngine
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EditTextInsertAdapterTest {
    @Test
    fun `insert appends operation and selects first placeholder`() {
        val editText = editText("Area: ")
        val operation = SnippetCatalog.builtIn()
            .first { it.id == "fraction" }
            .toInsertOperation()

        val insertionStart = EditTextInsertAdapter.insert(editText, operation)

        assertEquals(6, insertionStart)
        assertEquals("Area: \\frac{}{}", editText.text.toString())
        assertEquals(12, editText.selectionStart)
        assertEquals(12, editText.selectionEnd)
    }

    @Test
    fun `insert replaces selected text`() {
        val editText = editText("x plus y")
        editText.setSelection(2, 6)
        val operation = SnippetCatalog.builtIn()
            .first { it.id == "square-root" }
            .toInsertOperation()

        val insertionStart = EditTextInsertAdapter.insert(editText, operation)

        assertEquals(2, insertionStart)
        assertEquals("x \\sqrt{} y", editText.text.toString())
        assertEquals(8, editText.selectionStart)
        assertEquals(8, editText.selectionEnd)
    }

    @Test
    fun `insert selects default placeholder text`() {
        val editText = editText("")
        val operation = TemplateEngine.toInsertOperation(
            Template("x^{<|exponent=n>}"),
        )

        EditTextInsertAdapter.insert(editText, operation)

        assertEquals("x^{n}", editText.text.toString())
        assertEquals(3, editText.selectionStart)
        assertEquals(4, editText.selectionEnd)
    }

    private fun editText(text: String): EditText =
        EditText(RuntimeEnvironment.getApplication()).apply {
            setText(text)
            setSelection(text.length)
        }
}
