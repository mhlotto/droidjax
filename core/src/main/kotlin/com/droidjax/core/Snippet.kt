package com.droidjax.core

data class Snippet(
    val id: String,
    val title: String,
    val category: String,
    val templateBody: String,
    val aliases: List<String> = emptyList(),
    val previewText: String = templateBody,
    val accessibilityLabel: String = title,
) {
    fun toInsertOperation(): InsertOperation =
        TemplateEngine.toInsertOperation(
            template = Template(
                body = templateBody,
                id = id,
                title = title,
                category = category,
            ),
        )
}

data class SnippetCategory(
    val id: String,
    val title: String,
    val sortOrder: Int,
)

data class SnippetGroup(
    val category: SnippetCategory,
    val snippets: List<Snippet>,
)

data class Template(
    val body: String,
    val id: String? = null,
    val title: String? = null,
    val category: String? = null,
)
