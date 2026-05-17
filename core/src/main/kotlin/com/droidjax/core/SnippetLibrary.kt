package com.droidjax.core

data class UserSnippet(
    val id: String,
    val title: String,
    val category: String = SnippetCatalog.Category.Structure,
    val templateBody: String,
    val aliases: List<String> = emptyList(),
    val previewText: String = templateBody,
    val accessibilityLabel: String = title,
) {
    fun toSnippet(): Snippet = Snippet(
        id = id,
        title = title,
        category = category,
        templateBody = templateBody,
        aliases = aliases,
        previewText = previewText,
        accessibilityLabel = accessibilityLabel,
    )
}

data class SnippetPack(
    val id: String,
    val title: String,
    val snippets: List<UserSnippet>,
)

data class SnippetLibrary(
    val builtIns: List<Snippet> = SnippetCatalog.builtIn(),
    val userSnippets: List<UserSnippet> = emptyList(),
    val categories: List<SnippetCategory> = SnippetCatalog.categories(),
) {
    val snippets: List<Snippet>
        get() = builtIns + userSnippets.map { it.toSnippet() }

    fun validate(): ValidationResult<SnippetLibrary> {
        val catalogValidation = SnippetValidator.validateCatalog(snippets, categories)
        return if (catalogValidation.isValid) {
            ValidationResult.valid(this)
        } else {
            ValidationResult.invalid(catalogValidation.issues)
        }
    }

    fun search(query: String): List<Snippet> =
        SnippetCatalog.search(
            query = query,
            snippets = snippets,
        )

    fun rankedSearch(query: String): List<SnippetSearchResult> =
        SnippetCatalog.rankedSearch(
            query = query,
            snippets = snippets,
        )

    fun grouped(snippets: List<Snippet> = this.snippets): List<SnippetGroup> {
        val snippetsByCategory = snippets.groupBy { it.category }
        return categories.mapNotNull { category ->
            val categorySnippets = snippetsByCategory[category.id].orEmpty()
            if (categorySnippets.isEmpty()) {
                null
            } else {
                SnippetGroup(category, categorySnippets)
            }
        }
    }
}
