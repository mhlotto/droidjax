package com.droidjax.keyboard.ime

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import com.droidjax.android.common.DroidJaxStateStore
import com.droidjax.android.common.InputConnectionInsertAdapter
import com.droidjax.android.common.SharedPreferencesDroidJaxStateStore
import com.droidjax.core.DroidJaxState
import com.droidjax.core.Snippet

class DroidJaxInputMethodService : InputMethodService() {
    private lateinit var stateStore: DroidJaxStateStore

    private var state: DroidJaxState = DroidJaxState()

    override fun onCreate() {
        super.onCreate()
        stateStore = SharedPreferencesDroidJaxStateStore(
            sharedPreferences = getSharedPreferences(
                SharedPreferencesDroidJaxStateStore.DefaultName,
                Context.MODE_PRIVATE,
            ),
        )
        state = stateStore.load()
    }

    override fun onCreateInputView(): View {
        state = stateStore.load()

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
                    val committed = InputConnectionInsertAdapter.commit(
                        inputConnection = inputConnection,
                        operation = snippet.toInsertOperation(),
                    )
                    if (committed) {
                        state = state.recordSnippetUse(
                            snippetId = snippet.id,
                            usedAt = System.currentTimeMillis(),
                        )
                        stateStore.save(state)
                    }
                }
            }
        }

    private fun proofOfConceptSnippets(): List<Snippet> {
        val byId = state.activeSnippets.associateBy { it.id }
        return listOfNotNull(
            byId["inline-math"],
            byId["fraction"],
            byId["square-root"],
            byId["superscript"],
        )
    }
}
