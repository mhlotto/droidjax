package com.droidjax.core

sealed interface ValidationIssue {
    val message: String
}

data class ValidationResult<out T>(
    val value: T?,
    val issues: List<ValidationIssue> = emptyList(),
) {
    val isValid: Boolean
        get() = issues.isEmpty()

    companion object {
        fun <T> valid(value: T): ValidationResult<T> =
            ValidationResult(value = value)

        fun <T> invalid(issues: List<ValidationIssue>): ValidationResult<T> =
            ValidationResult(value = null, issues = issues)
    }
}

sealed interface TemplateValidationIssue : ValidationIssue

data class UnclosedPlaceholderMarker(
    val position: Int,
) : TemplateValidationIssue {
    override val message: String = "Placeholder marker at $position is missing a closing >."
}

data class EmptyPlaceholderLabel(
    val position: Int,
) : TemplateValidationIssue {
    override val message: String = "Placeholder label at $position is empty."
}

data class NestedPlaceholderMarker(
    val position: Int,
) : TemplateValidationIssue {
    override val message: String = "Placeholder marker at $position contains another < marker."
}

sealed interface SnippetValidationIssue : ValidationIssue

data class InvalidSnippetId(
    val id: String,
) : SnippetValidationIssue {
    override val message: String = "Snippet id must use lowercase letters, numbers, and hyphens: $id."
}

data class BlankSnippetTitle(
    val id: String,
) : SnippetValidationIssue {
    override val message: String = "Snippet title must not be blank for $id."
}

data class BlankSnippetCategory(
    val id: String,
) : SnippetValidationIssue {
    override val message: String = "Snippet category must not be blank for $id."
}

data class BlankSnippetTemplate(
    val id: String,
) : SnippetValidationIssue {
    override val message: String = "Snippet template must not be blank for $id."
}

data class UnknownSnippetCategory(
    val id: String,
    val category: String,
) : SnippetValidationIssue {
    override val message: String = "Snippet $id uses unknown category $category."
}

data class SnippetTemplateIssue(
    val id: String,
    val issue: TemplateValidationIssue,
) : SnippetValidationIssue {
    override val message: String = "Snippet $id has invalid template: ${issue.message}"
}

sealed interface CatalogValidationIssue : ValidationIssue

data class DuplicateSnippetId(
    val id: String,
) : CatalogValidationIssue {
    override val message: String = "Duplicate snippet id: $id."
}

data class InvalidCatalogSnippet(
    val issue: SnippetValidationIssue,
) : CatalogValidationIssue {
    override val message: String = issue.message
}

sealed interface DelimiterProfileValidationIssue : ValidationIssue

data class InvalidDelimiterProfileId(
    val id: String,
) : DelimiterProfileValidationIssue {
    override val message: String =
        "Delimiter profile id must use lowercase letters, numbers, and hyphens: $id."
}

data class BlankDelimiterProfileTitle(
    val id: String,
) : DelimiterProfileValidationIssue {
    override val message: String = "Delimiter profile title must not be blank for $id."
}

data class BlankInlineDelimiter(
    val id: String,
    val side: DelimiterSide,
) : DelimiterProfileValidationIssue {
    override val message: String = "Inline ${side.name.lowercase()} delimiter must not be blank for $id."
}

data class BlankDisplayDelimiter(
    val id: String,
    val side: DelimiterSide,
) : DelimiterProfileValidationIssue {
    override val message: String = "Display ${side.name.lowercase()} delimiter must not be blank for $id."
}

sealed interface DelimiterProfileLibraryValidationIssue : ValidationIssue

data class DuplicateDelimiterProfileId(
    val id: String,
) : DelimiterProfileLibraryValidationIssue {
    override val message: String = "Duplicate delimiter profile id: $id."
}

data class InvalidDelimiterProfile(
    val issue: DelimiterProfileValidationIssue,
) : DelimiterProfileLibraryValidationIssue {
    override val message: String = issue.message
}

enum class DelimiterSide {
    Open,
    Close,
}
