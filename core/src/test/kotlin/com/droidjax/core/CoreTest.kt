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

    private fun snippet(
        id: String,
        profile: DelimiterProfile = DelimiterProfile.DefaultMathJax,
    ): Snippet = SnippetCatalog.builtIn(profile).first { it.id == id }

    private fun <T> assertContains(values: Set<T>, expected: T) {
        assertTrue("Expected <$expected> in <$values>.", expected in values)
    }
}
