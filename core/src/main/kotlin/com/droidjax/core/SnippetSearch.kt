package com.droidjax.core

data class SnippetSearchResult(
    val snippet: Snippet,
    val score: Int,
    val match: SnippetSearchMatch,
)

enum class SnippetSearchMatch {
    EmptyQuery,
    ExactId,
    ExactTitle,
    TitlePrefix,
    AliasExact,
    AliasPrefix,
    IdContains,
    TitleContains,
    AliasContains,
    TexContains,
    PreviewContains,
    AccessibilityContains,
}
