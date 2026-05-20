package com.droidjax.keyboard.ime

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.droidjax.android.common.DroidJaxStateStore
import com.droidjax.android.common.InputConnectionInsertAdapter
import com.droidjax.android.common.SharedPreferencesDroidJaxStateStore
import com.droidjax.core.DelimiterProfile
import com.droidjax.core.DroidJaxState
import com.droidjax.core.Snippet

class DroidJaxInputMethodService : InputMethodService() {
    private lateinit var stateStore: DroidJaxStateStore
    private lateinit var root: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var modeContainer: LinearLayout
    private lateinit var categoryContainer: LinearLayout
    private lateinit var snippetContainer: LinearLayout

    private var state: DroidJaxState = DroidJaxState()
    private var filterMode: FilterMode = FilterMode.All
    private var selectedCategoryId: String? = null

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
        selectedCategoryId = null
        root = buildKeyboard()
        renderKeyboard()
        return root
    }

    override fun onStartInputView(
        info: android.view.inputmethod.EditorInfo?,
        restarting: Boolean,
    ) {
        super.onStartInputView(info, restarting)
        if (::root.isInitialized) {
            state = stateStore.load()
            renderKeyboard()
        }
    }

    private fun buildKeyboard(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(8))
            setBackgroundColor(Palette.Page)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )

            addView(headerRow())

            modeContainer = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            addView(modeContainer, matchWrap(topMargin = dp(6)))

            categoryContainer = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            addView(
                HorizontalScrollView(context).apply {
                    isHorizontalScrollBarEnabled = false
                    addView(categoryContainer)
                },
                matchWrap(topMargin = dp(6)),
            )

            snippetContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
            }
            addView(snippetContainer, matchWrap(topMargin = dp(6)))

            addView(utilityRow(), matchWrap(topMargin = dp(6)))
        }

    private fun headerRow(): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            addView(
                TextView(context).apply {
                    text = "DroidJax"
                    textSize = 15f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Palette.Text)
                },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
            )

            statusText = TextView(context).apply {
                textSize = 12f
                setTextColor(Palette.MutedText)
                gravity = Gravity.CENTER_VERTICAL or Gravity.END
            }
            addView(
                statusText,
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f),
            )

            addView(
                compactButton("Profile") {
                    toggleDelimiterProfile()
                },
                LinearLayout.LayoutParams(dp(76), dp(38)).apply {
                    leftMargin = dp(6)
                },
            )
        }

    private fun utilityRow(): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            addView(
                keyButton("Space") {
                    currentInputConnection?.commitText(" ", 1)
                },
                LinearLayout.LayoutParams(0, dp(42), 1.8f).apply {
                    rightMargin = dp(5)
                },
            )
            addView(
                keyButton("Enter") {
                    currentInputConnection?.commitText("\n", 1)
                },
                LinearLayout.LayoutParams(0, dp(42), 1f).apply {
                    rightMargin = dp(5)
                },
            )
            addView(
                keyButton("Back") {
                    sendBackspace(currentInputConnection)
                },
                LinearLayout.LayoutParams(0, dp(42), 1f),
            )
        }

    private fun renderKeyboard() {
        if (!::root.isInitialized) return

        statusText.text = "${state.activeDelimiterProfile.title} · ${visibleSnippets().size} keys"
        renderModes()
        renderCategories()
        renderSnippets()
    }

    private fun renderModes() {
        modeContainer.removeAllViews()
        FilterMode.entries.forEach { mode ->
            modeContainer.addView(
                compactButton(
                    label = mode.title,
                    selected = filterMode == mode,
                ) {
                    filterMode = mode
                    selectedCategoryId = null
                    renderKeyboard()
                },
                LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                    rightMargin = dp(5)
                },
            )
        }
    }

    private fun renderCategories() {
        categoryContainer.removeAllViews()

        val snippets = visibleSnippetsBeforeCategoryFilter()
        val countsByCategory = snippets.groupingBy { it.category }.eachCount()

        categoryContainer.addView(
            chipButton(
                label = "All ${snippets.size}",
                selected = selectedCategoryId == null,
            ) {
                selectedCategoryId = null
                renderKeyboard()
            },
        )

        state.activeSnippetLibrary().categories.forEach { category ->
            val count = countsByCategory[category.id] ?: 0
            if (count > 0) {
                categoryContainer.addView(
                    chipButton(
                        label = "${category.title} $count",
                        selected = selectedCategoryId == category.id,
                    ) {
                        selectedCategoryId = category.id
                        renderKeyboard()
                    },
                )
            }
        }
    }

    private fun renderSnippets() {
        snippetContainer.removeAllViews()
        val snippets = visibleSnippets().take(MaxVisibleKeys)

        if (snippets.isEmpty()) {
            snippetContainer.addView(
                TextView(this).apply {
                    text = when (filterMode) {
                        FilterMode.All -> "No snippets"
                        FilterMode.Favorites -> "No favorites saved"
                        FilterMode.Recents -> "No recent snippets"
                    }
                    gravity = Gravity.CENTER
                    setTextColor(Palette.MutedText)
                    setPadding(0, dp(14), 0, dp(14))
                },
            )
            return
        }

        snippets.chunked(KeysPerRow).forEach { rowSnippets ->
            snippetContainer.addView(snippetRow(rowSnippets))
        }
    }

    private fun snippetRow(snippets: List<Snippet>): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            snippets.forEach { snippet ->
                addView(
                    snippetKey(snippet),
                    LinearLayout.LayoutParams(0, dp(50), 1f).apply {
                        setMargins(dp(2), dp(2), dp(2), dp(2))
                    },
                )
            }
            repeat(KeysPerRow - snippets.size) {
                addView(View(context), LinearLayout.LayoutParams(0, dp(50), 1f))
            }
        }

    private fun snippetKey(snippet: Snippet): Button =
        Button(this).apply {
            text = snippet.previewText
            contentDescription = snippet.accessibilityLabel
            isAllCaps = false
            textSize = 12f
            minHeight = 0
            minWidth = 0
            setPadding(dp(4), 0, dp(4), 0)
            setTextColor(Palette.Text)
            background = rounded(Color.WHITE, Palette.Border)
            setOnClickListener {
                commitSnippet(snippet)
            }
            setOnLongClickListener {
                toggleFavorite(snippet)
                true
            }
        }

    private fun visibleSnippets(): List<Snippet> {
        val selectedCategory = selectedCategoryId
        return visibleSnippetsBeforeCategoryFilter()
            .filter { snippet -> selectedCategory == null || snippet.category == selectedCategory }
    }

    private fun visibleSnippetsBeforeCategoryFilter(): List<Snippet> =
        when (filterMode) {
            FilterMode.All -> state.activeSnippets
            FilterMode.Favorites -> state.favoriteSnippets()
            FilterMode.Recents -> state.recentSnippets()
        }

    private fun commitSnippet(snippet: Snippet) {
        val inputConnection = currentInputConnection ?: return
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
            renderKeyboard()
        }
    }

    private fun toggleFavorite(snippet: Snippet) {
        state = state.toggleFavorite(snippet.id)
        stateStore.save(state)
        renderKeyboard()
    }

    private fun toggleDelimiterProfile() {
        val nextProfileId = when (state.activeDelimiterProfileId) {
            DelimiterProfile.DefaultMathJax.id -> DelimiterProfile.DollarStyle.id
            else -> DelimiterProfile.DefaultMathJax.id
        }
        state = state.withActiveDelimiterProfile(nextProfileId)
        stateStore.save(state)
        renderKeyboard()
    }

    private fun sendBackspace(inputConnection: InputConnection?) {
        if (inputConnection == null) return

        val deleted = inputConnection.deleteSurroundingText(1, 0)
        if (!deleted) {
            inputConnection.sendKeyEvent(
                KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL),
            )
            inputConnection.sendKeyEvent(
                KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL),
            )
        }
    }

    private fun keyButton(
        label: String,
        onClick: () -> Unit,
    ): Button =
        compactButton(label = label, selected = false, onClick = onClick)

    private fun compactButton(
        label: String,
        selected: Boolean = false,
        onClick: () -> Unit,
    ): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 12f
            minHeight = 0
            minWidth = 0
            setPadding(dp(4), 0, dp(4), 0)
            setTextColor(if (selected) Palette.AccentText else Palette.Text)
            background = rounded(
                fillColor = if (selected) Palette.AccentSoft else Color.WHITE,
                strokeColor = if (selected) Palette.AccentStroke else Palette.Border,
            )
            setOnClickListener { onClick() }
        }

    private fun chipButton(
        label: String,
        selected: Boolean,
        onClick: () -> Unit,
    ): Button =
        compactButton(label = label, selected = selected, onClick = onClick).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(36),
            ).apply {
                rightMargin = dp(5)
            }
        }

    private fun matchWrap(topMargin: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply {
            this.topMargin = topMargin
        }

    private fun rounded(
        fillColor: Int,
        strokeColor: Int,
    ): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(8).toFloat()
            setColor(fillColor)
            setStroke(dp(1), strokeColor)
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private enum class FilterMode(
        val title: String,
    ) {
        All("All"),
        Favorites("Favorites"),
        Recents("Recents"),
    }

    private object Palette {
        const val Page: Int = 0xFFF6F7F4.toInt()
        const val Text: Int = 0xFF1E2428.toInt()
        const val MutedText: Int = 0xFF59656C.toInt()
        const val Border: Int = 0xFFD5DAD6.toInt()
        const val AccentSoft: Int = 0xFFE4F0EA.toInt()
        const val AccentStroke: Int = 0xFF8BB39D.toInt()
        const val AccentText: Int = 0xFF24533B.toInt()
    }

    private companion object {
        const val KeysPerRow = 4
        const val MaxVisibleKeys = 16
    }
}
