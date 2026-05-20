package com.droidjax.floatinghelper

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.droidjax.android.common.DroidJaxStateStore
import com.droidjax.android.common.EditTextInsertAdapter
import com.droidjax.android.common.SharedPreferencesDroidJaxStateStore
import com.droidjax.core.DelimiterProfile
import com.droidjax.core.DroidJaxState
import com.droidjax.core.InsertOperation
import com.droidjax.core.PlaceholderNavigator
import com.droidjax.core.Snippet
import com.droidjax.core.SnippetRef

class FloatingHelperActivity : Activity() {
    private lateinit var composer: EditText
    private lateinit var searchInput: EditText
    private lateinit var snippetContainer: LinearLayout
    private lateinit var modeFilterContainer: LinearLayout
    private lateinit var categoryFilterContainer: LinearLayout
    private lateinit var activeProfileText: TextView
    private lateinit var statusText: TextView
    private lateinit var stateStore: DroidJaxStateStore

    private var state: DroidJaxState = DroidJaxState()
    private var filterMode: FilterMode = FilterMode.All
    private var selectedCategoryId: String? = null
    private var lastOperation: InsertOperation? = null
    private var lastOperationStart: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        stateStore = SharedPreferencesDroidJaxStateStore(
            sharedPreferences = getSharedPreferences(
                SharedPreferencesDroidJaxStateStore.DefaultName,
                Context.MODE_PRIVATE,
            ),
        )
        state = stateStore.load()

        setContentView(buildContentView())
        renderAll()
    }

    private fun buildContentView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(16))
            setBackgroundColor(Palette.Page)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }

        root.addView(header())
        root.addView(delimiterControls(), matchWrap(topMargin = dp(8)))
        root.addView(modeFilters(), matchWrap(topMargin = dp(8)))
        root.addView(categoryFilters(), matchWrap(topMargin = dp(8)))
        root.addView(searchBox(), matchWrap(topMargin = dp(10)))
        root.addView(composerBox(), matchWrap(topMargin = dp(10)))
        root.addView(composerActions(), matchWrap(topMargin = dp(6)))

        statusText = TextView(this).apply {
            textSize = 13f
            setTextColor(Palette.MutedText)
            setPadding(0, dp(8), 0, 0)
        }
        root.addView(statusText, matchWrap())

        snippetContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(
            ScrollView(this).apply {
                isFillViewport = false
                addView(snippetContainer)
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ).apply {
                topMargin = dp(8)
            },
        )

        return root
    }

    private fun header(): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            addView(
                TextView(context).apply {
                    text = "DroidJax"
                    textSize = 24f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Palette.Text)
                },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
            )

            activeProfileText = TextView(context).apply {
                textSize = 13f
                gravity = Gravity.CENTER
                setTextColor(Palette.AccentText)
                setPadding(dp(10), dp(5), dp(10), dp(5))
                background = rounded(Palette.AccentSoft, Palette.AccentStroke)
            }
            addView(activeProfileText)
        }

    private fun delimiterControls(): View =
        RadioGroup(this).apply {
            orientation = RadioGroup.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(
                RadioButton(context).apply {
                    id = View.generateViewId()
                    text = "MathJax"
                    isChecked = state.activeDelimiterProfileId == DelimiterProfile.DefaultMathJax.id
                    setOnClickListener {
                        setActiveDelimiterProfile(DelimiterProfile.DefaultMathJax.id)
                    }
                },
            )
            addView(
                RadioButton(context).apply {
                    id = View.generateViewId()
                    text = "Dollar"
                    isChecked = state.activeDelimiterProfileId == DelimiterProfile.DollarStyle.id
                    setOnClickListener {
                        setActiveDelimiterProfile(DelimiterProfile.DollarStyle.id)
                    }
                },
            )
        }

    private fun modeFilters(): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            modeFilterContainer = this
        }

    private fun categoryFilters(): View =
        HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            categoryFilterContainer = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            addView(categoryFilterContainer)
        }

    private fun searchBox(): View {
        searchInput = EditText(this).apply {
            hint = "Search snippets"
            setSingleLine(true)
            textSize = 15f
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = rounded(Color.WHITE, Palette.Border)
            addTextChangedListener(
                object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int,
                    ) = Unit

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int,
                    ) {
                        selectedCategoryId = null
                        renderAll()
                    }

                    override fun afterTextChanged(s: Editable?) = Unit
                },
            )
        }
        return searchInput
    }

    private fun composerBox(): View {
        composer = EditText(this).apply {
            hint = "Compose TeX"
            minLines = 3
            gravity = Gravity.TOP or Gravity.START
            textSize = 15f
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = rounded(Color.WHITE, Palette.Border)
        }
        return composer
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
                    composer.requestFocus()
                },
            )
        }

    private fun renderAll() {
        if (!::snippetContainer.isInitialized) return

        activeProfileText.text = state.activeDelimiterProfile.title
        renderModeFilters()
        renderCategoryFilters()
        renderSnippets()
    }

    private fun renderModeFilters() {
        modeFilterContainer.removeAllViews()
        FilterMode.entries.forEach { mode ->
            modeFilterContainer.addView(
                filterButton(
                    label = mode.title,
                    selected = filterMode == mode,
                ) {
                    filterMode = mode
                    selectedCategoryId = null
                    renderAll()
                },
            )
        }
    }

    private fun renderCategoryFilters() {
        categoryFilterContainer.removeAllViews()

        val snippets = visibleSnippetsBeforeCategoryFilter()
        val countsByCategory = snippets.groupingBy { it.category }.eachCount()
        val allLabel = "All ${snippets.size}"
        categoryFilterContainer.addView(
            filterButton(
                label = allLabel,
                selected = selectedCategoryId == null,
            ) {
                selectedCategoryId = null
                renderAll()
            },
        )

        state.activeSnippetLibrary().categories.forEach { category ->
            val count = countsByCategory[category.id] ?: 0
            if (count > 0) {
                categoryFilterContainer.addView(
                    filterButton(
                        label = "${category.title} $count",
                        selected = selectedCategoryId == category.id,
                    ) {
                        selectedCategoryId = category.id
                        renderAll()
                    },
                )
            }
        }
    }

    private fun renderSnippets() {
        snippetContainer.removeAllViews()

        val snippets = visibleSnippets()
        statusText.text = statusMessage(snippets.size)

        val groups = state.grouped(snippets)
        groups.forEach { group ->
            snippetContainer.addView(sectionTitle(group.category.title))
            group.snippets.forEach { snippet ->
                snippetContainer.addView(snippetRow(snippet))
            }
        }

        if (groups.isEmpty()) {
            snippetContainer.addView(emptyMessage())
        }
    }

    private fun visibleSnippets(): List<Snippet> {
        val selectedCategory = selectedCategoryId
        return visibleSnippetsBeforeCategoryFilter()
            .filter { snippet ->
                selectedCategory == null || snippet.category == selectedCategory
            }
    }

    private fun visibleSnippetsBeforeCategoryFilter(): List<Snippet> {
        val modeSnippets = when (filterMode) {
            FilterMode.All -> state.activeSnippets
            FilterMode.Favorites -> state.favoriteSnippets()
            FilterMode.Recents -> state.recentSnippets()
        }
        val query = searchInput.text?.toString().orEmpty().trim()
        if (query.isEmpty()) {
            return modeSnippets
        }

        val allowedIds = modeSnippets.map { it.id }.toSet()
        return state.rankedSearch(query)
            .map { it.snippet }
            .filter { it.id in allowedIds }
    }

    private fun snippetRow(snippet: Snippet): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(3), 0, dp(3))

            addView(
                Button(context).apply {
                    text = "${snippet.previewText}\n${snippet.title}"
                    contentDescription = snippet.accessibilityLabel
                    isAllCaps = false
                    textSize = 13f
                    minHeight = dp(54)
                    gravity = Gravity.CENTER
                    setTextColor(Palette.Text)
                    background = rounded(Color.WHITE, Palette.Border)
                    setOnClickListener {
                        insertSnippet(snippet)
                    }
                },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    rightMargin = dp(6)
                },
            )

            addView(
                Button(context).apply {
                    val isFavorite = state.favorites.contains(SnippetRef(snippet.id))
                    text = if (isFavorite) "Saved" else "Save"
                    contentDescription = if (isFavorite) {
                        "Remove ${snippet.title} from favorites"
                    } else {
                        "Save ${snippet.title} as favorite"
                    }
                    isAllCaps = false
                    textSize = 12f
                    minHeight = dp(54)
                    setTextColor(if (isFavorite) Palette.AccentText else Palette.MutedText)
                    background = rounded(
                        fillColor = if (isFavorite) Palette.AccentSoft else Color.WHITE,
                        strokeColor = if (isFavorite) Palette.AccentStroke else Palette.Border,
                    )
                    setOnClickListener {
                        toggleFavorite(snippet)
                    }
                },
                LinearLayout.LayoutParams(dp(78), ViewGroup.LayoutParams.WRAP_CONTENT),
            )
        }

    private fun insertSnippet(snippet: Snippet) {
        val operation = snippet.toInsertOperation()
        lastOperationStart = EditTextInsertAdapter.insert(composer, operation)
        lastOperation = operation
        state = state.recordSnippetUse(
            snippetId = snippet.id,
            usedAt = System.currentTimeMillis(),
        )
        stateStore.save(state)
        composer.requestFocus()
        renderAll()
    }

    private fun toggleFavorite(snippet: Snippet) {
        state = state.toggleFavorite(snippet.id)
        stateStore.save(state)
        renderAll()
    }

    private fun setActiveDelimiterProfile(profileId: String) {
        state = state.withActiveDelimiterProfile(profileId)
        stateStore.save(state)
        lastOperation = null
        lastOperationStart = null
        renderAll()
    }

    private fun moveToNextPlaceholder() {
        val operation = lastOperation
        val operationStart = lastOperationStart
        if (operation == null || operationStart == null) {
            Toast.makeText(this, "No active snippet", Toast.LENGTH_SHORT).show()
            return
        }

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

    private fun statusMessage(snippetCount: Int): String {
        val modeText = filterMode.title.lowercase()
        val query = searchInput.text?.toString().orEmpty().trim()
        val queryText = if (query.isEmpty()) "" else " for \"$query\""
        return "$snippetCount $modeText snippets$queryText"
    }

    private fun emptyMessage(): TextView =
        TextView(this).apply {
            text = when (filterMode) {
                FilterMode.All -> "No snippets"
                FilterMode.Favorites -> "No favorites saved"
                FilterMode.Recents -> "No recent snippets"
            }
            gravity = Gravity.CENTER
            setTextColor(Palette.MutedText)
            setPadding(0, dp(28), 0, dp(28))
        }

    private fun sectionTitle(text: String): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Palette.Text)
            setPadding(0, dp(14), 0, dp(4))
        }

    private fun actionButton(
        label: String,
        onClick: () -> Unit,
    ): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            minHeight = dp(42)
            setTextColor(Palette.Text)
            background = rounded(Color.WHITE, Palette.Border)
            setOnClickListener { onClick() }
        }

    private fun filterButton(
        label: String,
        selected: Boolean,
        onClick: () -> Unit,
    ): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 13f
            minHeight = dp(38)
            setTextColor(if (selected) Palette.AccentText else Palette.MutedText)
            background = rounded(
                fillColor = if (selected) Palette.AccentSoft else Color.WHITE,
                strokeColor = if (selected) Palette.AccentStroke else Palette.Border,
            )
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                rightMargin = dp(6)
            }
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
}
