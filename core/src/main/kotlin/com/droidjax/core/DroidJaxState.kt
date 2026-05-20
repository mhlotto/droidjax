package com.droidjax.core

data class DroidJaxState(
    val snippetLibrary: SnippetLibrary = SnippetLibrary(),
    val delimiterProfileLibrary: DelimiterProfileLibrary = DelimiterProfileLibrary(),
    val activeDelimiterProfileId: String = DelimiterProfile.DefaultMathJax.id,
    val favorites: FavoriteSnippets = FavoriteSnippets(),
    val recents: RecentSnippets = RecentSnippets(),
) {
    val activeDelimiterProfile: DelimiterProfile
        get() = delimiterProfileLibrary.findById(activeDelimiterProfileId)
            ?: DelimiterProfile.DefaultMathJax

    val activeSnippets: List<Snippet>
        get() = SnippetCatalog.builtIn(activeDelimiterProfile) +
            snippetLibrary.allUserSnippets.map { it.toSnippet() }

    fun activeSnippetLibrary(): SnippetLibrary =
        snippetLibrary.copy(
            builtIns = SnippetCatalog.builtIn(activeDelimiterProfile),
        )

    fun search(query: String): List<Snippet> =
        SnippetCatalog.search(
            query = query,
            snippets = activeSnippets,
        )

    fun rankedSearch(query: String): List<SnippetSearchResult> =
        SnippetCatalog.rankedSearch(
            query = query,
            snippets = activeSnippets,
        )

    fun grouped(snippets: List<Snippet> = activeSnippets): List<SnippetGroup> =
        activeSnippetLibrary().grouped(snippets)

    fun favoriteSnippets(): List<Snippet> =
        favorites.resolve(activeSnippetLibrary())

    fun recentSnippets(): List<Snippet> =
        recents.resolve(activeSnippetLibrary())

    fun toggleFavorite(snippetId: String): DroidJaxState =
        copy(
            favorites = favorites.toggle(SnippetRef(snippetId)),
        )

    fun recordSnippetUse(
        snippetId: String,
        usedAt: Long,
    ): DroidJaxState =
        copy(
            recents = recents.recordUse(
                ref = SnippetRef(snippetId),
                usedAt = usedAt,
            ),
        )

    fun withActiveDelimiterProfile(profileId: String): DroidJaxState =
        copy(activeDelimiterProfileId = profileId)

    fun validate(): ValidationResult<DroidJaxState> {
        val issues = mutableListOf<DroidJaxStateValidationIssue>()

        snippetLibrary.validate().issues.forEach { issue ->
            issues += InvalidStateSnippetLibrary(issue)
        }
        delimiterProfileLibrary.validate().issues.forEach { issue ->
            issues += InvalidStateDelimiterProfileLibrary(issue)
        }
        if (delimiterProfileLibrary.findById(activeDelimiterProfileId) == null) {
            issues += MissingActiveDelimiterProfile(activeDelimiterProfileId)
        }

        return if (issues.isEmpty()) {
            ValidationResult.valid(this)
        } else {
            ValidationResult.invalid(issues)
        }
    }
}
