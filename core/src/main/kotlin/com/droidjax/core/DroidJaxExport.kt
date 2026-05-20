package com.droidjax.core

data class DroidJaxExport(
    val formatVersion: Int = CurrentFormatVersion,
    val userSnippets: List<UserSnippetExport> = emptyList(),
    val snippetPacks: List<SnippetPackExport> = emptyList(),
    val delimiterProfiles: List<DelimiterProfileExport> = emptyList(),
) {
    fun toSnippetLibrary(
        base: SnippetLibrary = SnippetLibrary(),
    ): SnippetLibrary =
        base.copy(
            userSnippets = userSnippets.map { it.toUserSnippet() },
            snippetPacks = snippetPacks.map { it.toSnippetPack() },
        )

    fun toDelimiterProfileLibrary(
        base: DelimiterProfileLibrary = DelimiterProfileLibrary(),
    ): DelimiterProfileLibrary =
        base.copy(
            userProfiles = delimiterProfiles.map { it.toDelimiterProfile() },
        )

    fun importInto(
        state: DroidJaxState = DroidJaxState(),
    ): ValidationResult<DroidJaxState> {
        val validation = validate()
        if (!validation.isValid) {
            return ValidationResult.invalid(validation.issues)
        }

        return ValidationResult.valid(
            state.copy(
                snippetLibrary = toSnippetLibrary(state.snippetLibrary),
                delimiterProfileLibrary = toDelimiterProfileLibrary(
                    state.delimiterProfileLibrary,
                ),
            ),
        )
    }

    fun validate(): ValidationResult<DroidJaxExport> =
        DroidJaxExportValidator.validate(this)

    companion object {
        const val CurrentFormatVersion = 1

        fun fromState(state: DroidJaxState): DroidJaxExport =
            DroidJaxExport(
                userSnippets = state.snippetLibrary.userSnippets.map {
                    UserSnippetExport.fromUserSnippet(it)
                },
                snippetPacks = state.snippetLibrary.snippetPacks.map {
                    SnippetPackExport.fromSnippetPack(it)
                },
                delimiterProfiles = state.delimiterProfileLibrary.userProfiles.map {
                    DelimiterProfileExport.fromDelimiterProfile(it)
                },
            )
    }
}

data class UserSnippetExport(
    val id: String,
    val title: String,
    val category: String = SnippetCatalog.Category.Structure,
    val templateBody: String,
    val aliases: List<String> = emptyList(),
    val previewText: String = templateBody,
    val accessibilityLabel: String = title,
) {
    fun toUserSnippet(): UserSnippet =
        UserSnippet(
            id = id,
            title = title,
            category = category,
            templateBody = templateBody,
            aliases = aliases,
            previewText = previewText,
            accessibilityLabel = accessibilityLabel,
        )

    companion object {
        fun fromUserSnippet(snippet: UserSnippet): UserSnippetExport =
            UserSnippetExport(
                id = snippet.id,
                title = snippet.title,
                category = snippet.category,
                templateBody = snippet.templateBody,
                aliases = snippet.aliases,
                previewText = snippet.previewText,
                accessibilityLabel = snippet.accessibilityLabel,
            )
    }
}

data class SnippetPackExport(
    val id: String,
    val title: String,
    val snippets: List<UserSnippetExport> = emptyList(),
) {
    fun toSnippetPack(): SnippetPack =
        SnippetPack(
            id = id,
            title = title,
            snippets = snippets.map { it.toUserSnippet() },
        )

    companion object {
        fun fromSnippetPack(pack: SnippetPack): SnippetPackExport =
            SnippetPackExport(
                id = pack.id,
                title = pack.title,
                snippets = pack.snippets.map { UserSnippetExport.fromUserSnippet(it) },
            )
    }
}

data class DelimiterProfileExport(
    val id: String,
    val title: String,
    val inlineOpen: String,
    val inlineClose: String,
    val displayOpen: String,
    val displayClose: String,
) {
    fun toDelimiterProfile(): DelimiterProfile =
        DelimiterProfile(
            id = id,
            title = title,
            inlineOpen = inlineOpen,
            inlineClose = inlineClose,
            displayOpen = displayOpen,
            displayClose = displayClose,
        )

    companion object {
        fun fromDelimiterProfile(profile: DelimiterProfile): DelimiterProfileExport =
            DelimiterProfileExport(
                id = profile.id,
                title = profile.title,
                inlineOpen = profile.inlineOpen,
                inlineClose = profile.inlineClose,
                displayOpen = profile.displayOpen,
                displayClose = profile.displayClose,
            )
    }
}

object DroidJaxExportValidator {
    private val packIdRegex = Regex("[a-z0-9]+(?:-[a-z0-9]+)*")

    fun validate(export: DroidJaxExport): ValidationResult<DroidJaxExport> {
        val issues = mutableListOf<DroidJaxExportValidationIssue>()

        if (export.formatVersion != DroidJaxExport.CurrentFormatVersion) {
            issues += UnsupportedExportFormatVersion(export.formatVersion)
        }

        validateSnippetPacks(export.snippetPacks).forEach { issue ->
            issues += issue
        }

        val snippetLibrary = export.toSnippetLibrary()
        snippetLibrary.validate().issues.forEach { issue ->
            issues += InvalidExportSnippetLibrary(issue)
        }

        val delimiterProfileLibrary = export.toDelimiterProfileLibrary()
        delimiterProfileLibrary.validate().issues.forEach { issue ->
            issues += InvalidExportDelimiterProfileLibrary(issue)
        }

        return if (issues.isEmpty()) {
            ValidationResult.valid(export)
        } else {
            ValidationResult.invalid(issues)
        }
    }

    private fun validateSnippetPacks(
        packs: List<SnippetPackExport>,
    ): List<DroidJaxExportValidationIssue> {
        val issues = mutableListOf<DroidJaxExportValidationIssue>()

        packs.groupBy { it.id }
            .filterValues { it.size > 1 }
            .keys
            .forEach { id -> issues += DuplicateSnippetPackId(id) }

        packs.forEach { pack ->
            if (!packIdRegex.matches(pack.id)) {
                issues += InvalidSnippetPackId(pack.id)
            }
            if (pack.title.isBlank()) {
                issues += BlankSnippetPackTitle(pack.id)
            }
        }

        return issues
    }
}
