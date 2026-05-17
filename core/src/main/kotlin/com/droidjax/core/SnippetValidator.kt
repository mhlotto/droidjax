package com.droidjax.core

object SnippetValidator {
    private val snippetIdRegex = Regex("[a-z0-9]+(?:-[a-z0-9]+)*")

    fun validateTemplate(template: Template): ValidationResult<Template> {
        val issues = validateTemplateBody(template.body)
        return if (issues.isEmpty()) {
            ValidationResult.valid(template)
        } else {
            ValidationResult.invalid(issues)
        }
    }

    fun validateSnippet(
        snippet: Snippet,
        categories: List<SnippetCategory> = SnippetCatalog.categories(),
    ): ValidationResult<Snippet> {
        val issues = mutableListOf<SnippetValidationIssue>()
        val knownCategoryIds = categories.map { it.id }.toSet()

        if (!snippetIdRegex.matches(snippet.id)) {
            issues += InvalidSnippetId(snippet.id)
        }
        if (snippet.title.isBlank()) {
            issues += BlankSnippetTitle(snippet.id)
        }
        if (snippet.category.isBlank()) {
            issues += BlankSnippetCategory(snippet.id)
        } else if (snippet.category !in knownCategoryIds) {
            issues += UnknownSnippetCategory(snippet.id, snippet.category)
        }
        if (snippet.templateBody.isBlank()) {
            issues += BlankSnippetTemplate(snippet.id)
        }

        validateTemplateBody(snippet.templateBody).forEach { issue ->
            issues += SnippetTemplateIssue(snippet.id, issue)
        }

        return if (issues.isEmpty()) {
            ValidationResult.valid(snippet)
        } else {
            ValidationResult.invalid(issues)
        }
    }

    fun validateCatalog(
        snippets: List<Snippet>,
        categories: List<SnippetCategory> = SnippetCatalog.categories(),
    ): ValidationResult<List<Snippet>> {
        val issues = mutableListOf<CatalogValidationIssue>()

        snippets.groupBy { it.id }
            .filterValues { it.size > 1 }
            .keys
            .forEach { id -> issues += DuplicateSnippetId(id) }

        snippets.forEach { snippet ->
            validateSnippet(snippet, categories).issues.forEach { issue ->
                issues += InvalidCatalogSnippet(issue as SnippetValidationIssue)
            }
        }

        return if (issues.isEmpty()) {
            ValidationResult.valid(snippets)
        } else {
            ValidationResult.invalid(issues)
        }
    }

    private fun validateTemplateBody(body: String): List<TemplateValidationIssue> {
        val issues = mutableListOf<TemplateValidationIssue>()
        var index = 0

        while (index < body.length) {
            if (body[index] != '<') {
                index += 1
                continue
            }

            val closeIndex = body.indexOf('>', startIndex = index + 1)
            if (closeIndex == -1) {
                issues += UnclosedPlaceholderMarker(index)
                index += 1
                continue
            }

            val rawContent = body.substring(index + 1, closeIndex)
            if ('<' in rawContent) {
                issues += NestedPlaceholderMarker(index)
            }

            val content = if (rawContent.startsWith("|")) rawContent.drop(1) else rawContent
            val defaultSeparatorIndex = content.indexOf('=')
            val label = when {
                defaultSeparatorIndex == -1 -> content
                else -> content.substring(0, defaultSeparatorIndex)
            }.trim()
            if (content.isNotEmpty() && label.isEmpty()) {
                issues += EmptyPlaceholderLabel(index)
            }

            index = closeIndex + 1
        }

        return issues
    }
}
