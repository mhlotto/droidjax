package com.droidjax.core

data class Snippet(
    val id: String,
    val title: String,
    val category: String,
    val templateBody: String,
    val aliases: List<String> = emptyList(),
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

data class Template(
    val body: String,
    val id: String? = null,
    val title: String? = null,
    val category: String? = null,
)
