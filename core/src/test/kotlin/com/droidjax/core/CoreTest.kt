package com.droidjax.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreTest {
    @Test
    fun defaultInlineMathOperationUsesMathJaxDelimiters() {
        val operation = snippet("inline-math").toInsertOperation()

        assertEquals("\\(\\)", operation.text)
        assertEquals(2, operation.initialCursorPosition)
        assertEquals(2, operation.cursorOffsetFromEnd)
        assertEquals(listOf(2..2), operation.placeholderRanges)
    }

    @Test
    fun dollarInlineMathOperationUsesDollarDelimiters() {
        val operation = snippet(
            id = "inline-math",
            profile = DelimiterProfile.DollarStyle,
        ).toInsertOperation()

        assertEquals("$$", operation.text)
        assertEquals(1, operation.initialCursorPosition)
        assertEquals(1, operation.cursorOffsetFromEnd)
        assertEquals(listOf(1..1), operation.placeholderRanges)
    }

    @Test
    fun displayMathOperationUsesActiveDelimiterProfile() {
        val defaultOperation = snippet("display-math").toInsertOperation()
        val dollarOperation = snippet(
            id = "display-math",
            profile = DelimiterProfile.DollarStyle,
        ).toInsertOperation()

        assertEquals("\\[\\]", defaultOperation.text)
        assertEquals(2, defaultOperation.initialCursorPosition)
        assertEquals(2, defaultOperation.cursorOffsetFromEnd)
        assertEquals(listOf(2..2), defaultOperation.placeholderRanges)

        assertEquals("$$$$", dollarOperation.text)
        assertEquals(2, dollarOperation.initialCursorPosition)
        assertEquals(2, dollarOperation.cursorOffsetFromEnd)
        assertEquals(listOf(2..2), dollarOperation.placeholderRanges)
    }

    @Test
    fun fractionOperationPlacesCursorAndPlaceholdersInsideBraces() {
        val operation = snippet("fraction").toInsertOperation()

        assertEquals("\\frac{}{}", operation.text)
        assertEquals(6, operation.initialCursorPosition)
        assertEquals(3, operation.cursorOffsetFromEnd)
        assertEquals(listOf(6..6, 8..8), operation.placeholderRanges)
        assertEquals(listOf("numerator", "denominator"), operation.placeholders.map { it.label })
    }

    @Test
    fun superscriptAndSubscriptOperationsPlaceCursorInsideFirstScript() {
        val superscript = snippet("superscript").toInsertOperation()
        val subscript = snippet("subscript").toInsertOperation()
        val combined = snippet("superscript-subscript").toInsertOperation()

        assertEquals("^{}", superscript.text)
        assertEquals(2, superscript.initialCursorPosition)
        assertEquals(1, superscript.cursorOffsetFromEnd)

        assertEquals("_{}", subscript.text)
        assertEquals(2, subscript.initialCursorPosition)
        assertEquals(1, subscript.cursorOffsetFromEnd)

        assertEquals("_{}^{}", combined.text)
        assertEquals(2, combined.initialCursorPosition)
        assertEquals(4, combined.cursorOffsetFromEnd)
        assertEquals(listOf(2..2, 5..5), combined.placeholderRanges)
    }

    @Test
    fun catalogContainsExpectedCategories() {
        val categories = SnippetCatalog.builtIn().map { it.category }.toSet()

        assertContains(categories, SnippetCatalog.Category.Delimiters)
        assertContains(categories, SnippetCatalog.Category.Structure)
        assertContains(categories, SnippetCatalog.Category.Scripts)
        assertContains(categories, SnippetCatalog.Category.Greek)
        assertContains(categories, SnippetCatalog.Category.Operators)
        assertContains(categories, SnippetCatalog.Category.Relations)
        assertContains(categories, SnippetCatalog.Category.Arrows)
        assertContains(categories, SnippetCatalog.Category.Functions)
        assertContains(categories, SnippetCatalog.Category.Accents)
        assertContains(categories, SnippetCatalog.Category.Sets)
    }

    @Test
    fun catalogCategoriesHaveStableDisplayOrder() {
        val categories = SnippetCatalog.categories()

        assertEquals(SnippetCatalog.Category.Delimiters, categories.first().id)
        assertEquals("Delimiters", categories.first().title)
        assertEquals(categories.map { it.sortOrder }, categories.map { it.sortOrder }.sorted())
    }

    @Test
    fun groupedCatalogUsesCategoryOrderAndOmitsEmptyGroups() {
        val groups = SnippetCatalog.groupedBuiltIn(
            snippets = listOf(snippet("fraction"), snippet("alpha")),
        )

        assertEquals(
            listOf(SnippetCatalog.Category.Structure, SnippetCatalog.Category.Greek),
            groups.map { it.category.id },
        )
        assertEquals(listOf("fraction"), groups.first().snippets.map { it.id })
    }

    @Test
    fun snippetsExposeUiPreviewAndAccessibilityText() {
        val alpha = snippet("alpha")
        val fraction = snippet("fraction")

        assertEquals("α", alpha.previewText)
        assertEquals("alpha", alpha.accessibilityLabel)
        assertEquals("a/b", fraction.previewText)
        assertEquals("Fraction", fraction.accessibilityLabel)
    }

    @Test
    fun searchFindsSnippetsByTitleAliasTexBodyAndId() {
        assertContains(
            SnippetCatalog.search("fraction").map { it.id }.toSet(),
            "fraction",
        )
        assertContains(
            SnippetCatalog.search("division").map { it.id }.toSet(),
            "fraction",
        )
        assertContains(
            SnippetCatalog.search("\\approx").map { it.id }.toSet(),
            "approximately",
        )
        assertContains(
            SnippetCatalog.search("maps-to").map { it.id }.toSet(),
            "maps-to",
        )
        assertContains(
            SnippetCatalog.search("α").map { it.id }.toSet(),
            "alpha",
        )
        assertContains(
            SnippetCatalog.search("ℝ").map { it.id }.toSet(),
            "real-numbers",
        )
    }

    @Test
    fun placeholderNavigationMovesThroughRangesThenAfterSnippet() {
        val fraction = snippet("fraction").toInsertOperation()
        val combined = snippet("superscript-subscript").toInsertOperation()

        assertEquals(6..6, PlaceholderNavigator.nextPlaceholder(fraction, 0))
        assertEquals(8..8, PlaceholderNavigator.nextPlaceholder(fraction, 6))
        assertNull(PlaceholderNavigator.nextPlaceholder(fraction, 8))
        assertEquals(8, PlaceholderNavigator.nextCursorPosition(fraction, 6))
        assertEquals(9, PlaceholderNavigator.nextCursorPosition(fraction, 8))

        assertEquals(5, PlaceholderNavigator.nextCursorPosition(combined, 2))
        assertEquals(6, PlaceholderNavigator.nextCursorPosition(combined, 5))
    }

    @Test
    fun templateEngineSupportsLabeledPlaceholdersAndDefaults() {
        val operation = TemplateEngine.toInsertOperation(
            Template("\\frac{<|numerator=n>}{<denominator=d>}"),
        )

        assertEquals("\\frac{n}{d}", operation.text)
        assertEquals(6, operation.initialCursorPosition)
        assertEquals(listOf(6..6, 9..9), operation.placeholderRanges)
        assertEquals("numerator", operation.placeholders[0].label)
        assertEquals("n", operation.placeholders[0].defaultText)
        assertEquals(6, operation.placeholders[0].start)
        assertEquals(7, operation.placeholders[0].end)
        assertEquals("denominator", operation.placeholders[1].label)
        assertEquals("d", operation.placeholders[1].defaultText)
    }

    @Test
    fun templateEngineSupportsMultiCharacterDefaultSelections() {
        val operation = TemplateEngine.toInsertOperation(
            Template("\\hat{<|value=theta>}"),
        )

        assertEquals("\\hat{theta}", operation.text)
        assertEquals(5, operation.initialCursorPosition)
        assertEquals(5, operation.placeholderRanges.first().count())
        assertEquals(5, operation.placeholders.first().start)
        assertEquals(10, operation.placeholders.first().end)
    }

    @Test
    fun placeholderSessionTracksCurrentPlaceholderAndFinalCursor() {
        val operation = snippet("fraction").toInsertOperation()
        val session = PlaceholderSession.start(operation)
        val second = session.next()
        val final = second.next()

        assertEquals("numerator", session.currentPlaceholder?.label)
        assertEquals(6, session.cursorPosition)
        assertEquals("denominator", second.currentPlaceholder?.label)
        assertEquals(8, second.cursorPosition)
        assertEquals(9, final.cursorPosition)
        assertTrue(final.isComplete)
    }

    @Test
    fun expandedCatalogContainsCommonMultiPlaceholderSnippets() {
        val snippets = SnippetCatalog.builtIn().associateBy { it.id }

        assertEquals(
            "\\begin{matrix} &  \\\\  & \\end{matrix}",
            snippets.getValue("matrix-2x2").toInsertOperation().text,
        )
        assertEquals(
            "\\begin{cases} & \\end{cases}",
            snippets.getValue("cases").toInsertOperation().text,
        )
        assertEquals("\\sin{x}", snippets.getValue("sin").toInsertOperation().text)
        assertEquals("\\vec{x}", snippets.getValue("vec").toInsertOperation().text)
        assertEquals("\\mathbb{R}", snippets.getValue("real-numbers").toInsertOperation().text)
    }

    private fun snippet(
        id: String,
        profile: DelimiterProfile = DelimiterProfile.DefaultMathJax,
    ): Snippet = SnippetCatalog.builtIn(profile).first { it.id == id }

    private fun <T> assertContains(values: Set<T>, expected: T) {
        assertTrue("Expected <$expected> in <$values>.", expected in values)
    }
}
