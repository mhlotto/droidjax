package com.droidjax.floatinghelper

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.droidjax.android.common.EditTextInsertAdapter
import com.droidjax.core.DelimiterProfile
import com.droidjax.core.InsertOperation
import com.droidjax.core.PlaceholderNavigator
import com.droidjax.core.Snippet
import com.droidjax.core.SnippetCatalog

class FloatingHelperActivity : Activity() {
    private lateinit var composer: EditText
    private lateinit var searchInput: EditText
    private lateinit var snippetContainer: LinearLayout

    private var delimiterProfile: DelimiterProfile = DelimiterProfile.DefaultMathJax
    private var lastOperation: InsertOperation? = null
    private var lastOperationStart: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(buildContentView())
        renderSnippets()
    }

    private fun buildContentView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(16))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }

        root.addView(title("DroidJax Helper"))
        root.addView(delimiterControls())

        searchInput = EditText(this).apply {
            hint = "Search snippets"
            setSingleLine(true)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            addTextChangedListener(
                object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        renderSnippets()
                    }

                    override fun afterTextChanged(s: Editable?) = Unit
                },
            )
        }
        root.addView(searchInput, matchWrap())

        composer = EditText(this).apply {
            hint = "Compose TeX"
            minLines = 3
            gravity = Gravity.TOP or Gravity.START
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        root.addView(composer, matchWrap(topMargin = dp(10)))

        root.addView(composerActions())

        snippetContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(
            ScrollView(this).apply {
                addView(snippetContainer)
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ).apply {
                topMargin = dp(12)
            },
        )

        return root
    }

    private fun delimiterControls(): View =
        RadioGroup(this).apply {
            orientation = RadioGroup.HORIZONTAL
            addView(
                RadioButton(context).apply {
                    id = View.generateViewId()
                    text = "MathJax"
                    isChecked = true
                    setOnClickListener {
                        delimiterProfile = DelimiterProfile.DefaultMathJax
                        renderSnippets()
                    }
                },
            )
            addView(
                RadioButton(context).apply {
                    id = View.generateViewId()
                    text = "Dollar"
                    setOnClickListener {
                        delimiterProfile = DelimiterProfile.DollarStyle
                        renderSnippets()
                    }
                },
            )
        }

    private fun composerActions(): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END

            addView(
                actionButton("Next") {
                    moveToNextPlaceholder()
                },
            )
            addView(
                actionButton("Copy") {
                    copyComposerText()
                },
            )
            addView(
                actionButton("Clear") {
                    composer.text.clear()
                    lastOperation = null
                    lastOperationStart = null
                },
            )
        }

    private fun renderSnippets() {
        if (!::snippetContainer.isInitialized) return

        snippetContainer.removeAllViews()

        val query = searchInput.text?.toString().orEmpty()
        val snippets = SnippetCatalog.search(
            query = query,
            snippets = SnippetCatalog.builtIn(delimiterProfile),
        )
        val groups = SnippetCatalog.groupedBuiltIn(
            delimiterProfile = delimiterProfile,
            snippets = snippets,
        )

        groups.forEach { group ->
            snippetContainer.addView(sectionTitle(group.category.title))

            val section = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

            group.snippets.chunked(3).forEach { rowSnippets ->
                section.addView(snippetRow(rowSnippets))
            }

            snippetContainer.addView(section)
        }

        if (groups.isEmpty()) {
            snippetContainer.addView(
                TextView(this).apply {
                    text = "No snippets"
                    gravity = Gravity.CENTER
                    setPadding(0, dp(24), 0, dp(24))
                },
            )
        }
    }

    private fun snippetRow(snippets: List<Snippet>): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            snippets.forEach { snippet ->
                addView(snippetButton(snippet))
            }
            repeat(3 - snippets.size) {
                addView(View(context), LinearLayout.LayoutParams(0, 1, 1f))
            }
        }

    private fun snippetButton(snippet: Snippet): View =
        Button(this).apply {
            text = snippet.previewText
            contentDescription = snippet.accessibilityLabel
            isAllCaps = false
            setOnClickListener {
                val operation = snippet.toInsertOperation()
                lastOperationStart = EditTextInsertAdapter.insert(composer, operation)
                lastOperation = operation
                composer.requestFocus()
            }
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f,
            ).apply {
                setMargins(dp(3), dp(3), dp(3), dp(3))
            }
        }

    private fun moveToNextPlaceholder() {
        val operation = lastOperation ?: return
        val operationStart = lastOperationStart ?: return
        val relativeCursor = composer.selectionStart - operationStart
        val nextCursor = operationStart + PlaceholderNavigator.nextCursorPosition(operation, relativeCursor)
        composer.setSelection(nextCursor.coerceIn(0, composer.text.length))
        composer.requestFocus()
    }

    private fun copyComposerText() {
        val text = composer.text.toString()
        if (text.isBlank()) {
            Toast.makeText(this, "Nothing to copy", Toast.LENGTH_SHORT).show()
            return
        }

        getSystemService(ClipboardManager::class.java).setPrimaryClip(
            ClipData.newPlainText("DroidJax TeX", text),
        )
        Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
    }

    private fun title(text: String): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 22f
            setPadding(0, 0, 0, dp(8))
        }

    private fun sectionTitle(text: String): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 16f
            setPadding(0, dp(16), 0, dp(4))
        }

    private fun actionButton(
        label: String,
        onClick: () -> Unit,
    ): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            setOnClickListener { onClick() }
        }

    private fun matchWrap(
        topMargin: Int = 0,
    ): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply {
            this.topMargin = topMargin
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
