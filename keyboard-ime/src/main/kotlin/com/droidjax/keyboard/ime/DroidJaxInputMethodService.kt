package com.droidjax.keyboard.ime

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import com.droidjax.android.common.InputConnectionInsertAdapter
import com.droidjax.core.Snippet
import com.droidjax.core.SnippetCatalog

class DroidJaxInputMethodService : InputMethodService() {
    override fun onCreateInputView(): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            setPadding(8, 8, 8, 8)
        }

        proofOfConceptSnippets().forEach { snippet ->
            row.addView(snippetButton(snippet))
        }

        return row
    }

    private fun snippetButton(snippet: Snippet): Button =
        Button(this).apply {
            text = snippet.title
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f,
            )
            setOnClickListener {
                currentInputConnection?.let { inputConnection ->
                    InputConnectionInsertAdapter.commit(
                        inputConnection = inputConnection,
                        operation = snippet.toInsertOperation(),
                    )
                }
            }
        }

    private fun proofOfConceptSnippets(): List<Snippet> {
        val byId = SnippetCatalog.builtIn().associateBy { it.id }
        return listOfNotNull(
            byId["inline-math"],
            byId["fraction"],
            byId["square-root"],
            byId["superscript"],
        )
    }
}
