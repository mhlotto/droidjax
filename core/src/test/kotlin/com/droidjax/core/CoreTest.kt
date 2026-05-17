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

    @Test
    fun allBuiltInSnippetsHaveUniqueIdsKnownCategoriesAndValidTemplates() {
        val snippets = SnippetCatalog.builtIn()
        val validation = SnippetValidator.validateCatalog(snippets)

        assertTrue(validation.issues.joinToString { it.message }, validation.isValid)
        assertEquals(snippets.size, snippets.map { it.id }.toSet().size)
        snippets.forEach { snippet ->
            val operation = snippet.toInsertOperation()
            assertTrue(operation.cursorOffsetFromEnd in 0..operation.text.length)
            operation.placeholders.forEach { placeholder ->
                assertTrue(placeholder.start in 0..operation.text.length)
                assertTrue(placeholder.end in 0..operation.text.length)
            }
        }
    }

    @Test
    fun templateValidationReportsMalformedMarkers() {
        val unclosed = SnippetValidator.validateTemplate(Template("\\frac{<|numerator}{}"))
        val emptyLabel = SnippetValidator.validateTemplate(Template("\\sqrt{<|=x>}"))
        val nested = SnippetValidator.validateTemplate(Template("\\sqrt{<outer<inner>>}"))

        assertContainsIssue<UnclosedPlaceholderMarker>(unclosed.issues)
        assertContainsIssue<EmptyPlaceholderLabel>(emptyLabel.issues)
        assertContainsIssue<NestedPlaceholderMarker>(nested.issues)
    }

    @Test
    fun snippetValidationReportsBadMetadataAndTemplateErrors() {
        val validation = SnippetValidator.validateSnippet(
            Snippet(
                id = "Bad Id",
                title = "",
                category = "missing",
                templateBody = "\\sqrt{<|=x>}",
            ),
        )

        assertContainsIssue<InvalidSnippetId>(validation.issues)
        assertContainsIssue<BlankSnippetTitle>(validation.issues)
        assertContainsIssue<UnknownSnippetCategory>(validation.issues)
        assertContainsIssue<SnippetTemplateIssue>(validation.issues)
    }

    @Test
    fun catalogValidationReportsDuplicateIds() {
        val validation = SnippetValidator.validateCatalog(
            listOf(
                snippet("fraction"),
                snippet("fraction"),
            ),
        )

        assertContainsIssue<DuplicateSnippetId>(validation.issues)
    }

    @Test
    fun snippetLibraryCombinesBuiltInsAndUserSnippets() {
        val userSnippet = UserSnippet(
            id = "quadratic-formula",
            title = "Quadratic Formula",
            category = SnippetCatalog.Category.Structure,
            templateBody = "x = \\frac{<|term=-b> \\pm \\sqrt{<radicand=b^2-4ac>}}{<denominator=2a>}",
            aliases = listOf("quadratic"),
            previewText = "quadratic",
        )
        val library = SnippetLibrary(userSnippets = listOf(userSnippet))

        assertTrue(library.validate().isValid)
        assertContains(library.search("quadratic").map { it.id }.toSet(), "quadratic-formula")
        assertContains(
            library.grouped().flatMap { it.snippets }.map { it.id }.toSet(),
            "quadratic-formula",
        )
        assertEquals(
            "x = \\frac{-b \\pm \\sqrt{b^2-4ac}}{2a}",
            userSnippet.toSnippet().toInsertOperation().text,
        )
    }

    @Test
    fun snippetLibraryValidationIncludesUserSnippetProblems() {
        val library = SnippetLibrary(
            userSnippets = listOf(
                UserSnippet(
                    id = "fraction",
                    title = "Duplicate Fraction",
                    templateBody = "<|value>",
                ),
                UserSnippet(
                    id = "bad user id",
                    title = "",
                    templateBody = "<|=x>",
                ),
            ),
        )
        val validation = library.validate()

        assertContainsIssue<DuplicateSnippetId>(validation.issues)
        assertContainsIssue<InvalidCatalogSnippet>(validation.issues)
    }

    @Test
    fun textComposerInsertsSnippetAndMovesThroughPlaceholders() {
        val composer = TextComposer("before  after", selectionStart = 7)
            .insert(snippet("fraction"))
        val second = composer.nextPlaceholder()
        val final = second.nextPlaceholder()

        assertEquals("before \\frac{}{} after", composer.text)
        assertEquals(13, composer.selectionStart)
        assertEquals(13, composer.selectionEnd)
        assertEquals(15, second.selectionStart)
        assertEquals(15, second.selectionEnd)
        assertEquals(16, final.selectionStart)
        assertEquals(16, final.selectionEnd)
    }

    @Test
    fun textComposerReplacesSelectionAndSelectsDefaultPlaceholderText() {
        val composer = TextComposer("abcXYZdef", selectionStart = 3, selectionEnd = 6)
            .insert(snippet("sin"))

        assertEquals("abc\\sin{x}def", composer.text)
        assertEquals(8, composer.selectionStart)
        assertEquals(9, composer.selectionEnd)
    }

    @Test
    fun rankedSearchOrdersExactAndPrefixMatchesBeforeFallbacks() {
        val results = SnippetCatalog.rankedSearch("limit")

        assertEquals("limit", results.first().snippet.id)
        assertEquals(SnippetSearchMatch.ExactId, results.first().match)
        assertContains(results.map { it.snippet.id }.toSet(), "superscript-subscript")
        assertTrue(
            results.first { it.snippet.id == "limit" }.score >
                results.first { it.snippet.id == "superscript-subscript" }.score,
        )
    }

    @Test
    fun rankedSearchReportsMatchKindForPreviewAndAliasMatches() {
        val previewResult = SnippetCatalog.rankedSearch("ℝ").first()
        val aliasResult = SnippetCatalog.rankedSearch("division").first()

        assertEquals("real-numbers", previewResult.snippet.id)
        assertEquals(SnippetSearchMatch.PreviewContains, previewResult.match)
        assertEquals("fraction", aliasResult.snippet.id)
        assertEquals(SnippetSearchMatch.AliasExact, aliasResult.match)
    }

    @Test
    fun snippetLibraryExposesRankedSearchAcrossUserSnippets() {
        val library = SnippetLibrary(
            userSnippets = listOf(
                UserSnippet(
                    id = "custom-limit",
                    title = "Custom Limit",
                    templateBody = "\\lim_{<|variable=x> \\to <target=0>}",
                    aliases = listOf("limit"),
                ),
            ),
        )
        val result = library.rankedSearch("custom").first()

        assertEquals("custom-limit", result.snippet.id)
        assertEquals(SnippetSearchMatch.TitlePrefix, result.match)
    }

    @Test
    fun favoritesPreserveInsertionOrderAndIgnoreDuplicates() {
        val fraction = SnippetRef("fraction")
        val sqrt = SnippetRef("square-root")
        val favorites = FavoriteSnippets()
            .add(fraction)
            .add(sqrt)
            .add(fraction)

        assertEquals(listOf(fraction, sqrt), favorites.refs)
        assertTrue(favorites.contains(fraction))
    }

    @Test
    fun favoritesToggleAndResolveAgainstLibrary() {
        val fraction = SnippetRef("fraction")
        val sqrt = SnippetRef("square-root")
        val favorites = FavoriteSnippets()
            .toggle(fraction)
            .toggle(sqrt)
            .toggle(fraction)

        assertEquals(listOf(sqrt), favorites.refs)
        assertEquals(
            listOf("square-root"),
            favorites.resolve(SnippetLibrary()).map { it.id },
        )
    }

    @Test
    fun recentsRecordMostRecentFirstAndIncrementUseCount() {
        val fraction = SnippetRef("fraction")
        val sqrt = SnippetRef("square-root")
        val recents = RecentSnippets(maxSize = 5)
            .recordUse(fraction, usedAt = 10)
            .recordUse(sqrt, usedAt = 20)
            .recordUse(fraction, usedAt = 30)

        assertEquals(listOf(fraction, sqrt), recents.items.map { it.ref })
        assertEquals(2, recents.items.first { it.ref == fraction }.useCount)
        assertEquals(30, recents.items.first { it.ref == fraction }.lastUsedAt)
    }

    @Test
    fun recentsTrimToMaxSizeAndTieBreakById() {
        val recents = RecentSnippets(maxSize = 2)
            .recordUse(SnippetRef("gamma"), usedAt = 10)
            .recordUse(SnippetRef("alpha"), usedAt = 10)
            .recordUse(SnippetRef("beta"), usedAt = 20)

        assertEquals(
            listOf("beta", "alpha"),
            recents.items.map { it.ref.id },
        )
    }

    @Test
    fun recentsRemoveClearAndResolveAgainstLibrary() {
        val fraction = SnippetRef("fraction")
        val missing = SnippetRef("missing")
        val recents = RecentSnippets(maxSize = 5)
            .recordUse(fraction, usedAt = 10)
            .recordUse(missing, usedAt = 20)
            .remove(missing)

        assertEquals(listOf("fraction"), recents.resolve(SnippetLibrary()).map { it.id })
        assertTrue(recents.clear().items.isEmpty())
    }

    private fun snippet(
        id: String,
        profile: DelimiterProfile = DelimiterProfile.DefaultMathJax,
    ): Snippet = SnippetCatalog.builtIn(profile).first { it.id == id }

    private fun <T> assertContains(values: Set<T>, expected: T) {
        assertTrue("Expected <$expected> in <$values>.", expected in values)
    }

    private inline fun <reified T : ValidationIssue> assertContainsIssue(issues: List<ValidationIssue>) {
        assertTrue(
            "Expected issue ${T::class.simpleName} in ${issues.map { it.message }}.",
            issues.any { it is T },
        )
    }
}
