package com.droidjax.core

data class DelimiterProfileLibrary(
    val builtIns: List<DelimiterProfile> = defaultProfiles(),
    val userProfiles: List<DelimiterProfile> = emptyList(),
) {
    val profiles: List<DelimiterProfile>
        get() = builtIns + userProfiles

    fun findById(id: String): DelimiterProfile? =
        profiles.firstOrNull { it.id == id }

    fun validate(): ValidationResult<DelimiterProfileLibrary> {
        val issues = mutableListOf<DelimiterProfileLibraryValidationIssue>()

        profiles.groupBy { it.id }
            .filterValues { it.size > 1 }
            .keys
            .forEach { id -> issues += DuplicateDelimiterProfileId(id) }

        profiles.forEach { profile ->
            DelimiterProfileValidator.validate(profile).issues.forEach { issue ->
                issues += InvalidDelimiterProfile(issue as DelimiterProfileValidationIssue)
            }
        }

        return if (issues.isEmpty()) {
            ValidationResult.valid(this)
        } else {
            ValidationResult.invalid(issues)
        }
    }

    companion object {
        fun defaultProfiles(): List<DelimiterProfile> =
            listOf(
                DelimiterProfile.DefaultMathJax,
                DelimiterProfile.DollarStyle,
            )
    }
}

object DelimiterProfileValidator {
    private val profileIdRegex = Regex("[a-z0-9]+(?:-[a-z0-9]+)*")

    fun validate(profile: DelimiterProfile): ValidationResult<DelimiterProfile> {
        val issues = mutableListOf<DelimiterProfileValidationIssue>()

        if (!profileIdRegex.matches(profile.id)) {
            issues += InvalidDelimiterProfileId(profile.id)
        }
        if (profile.title.isBlank()) {
            issues += BlankDelimiterProfileTitle(profile.id)
        }
        if (profile.inlineOpen.isBlank()) {
            issues += BlankInlineDelimiter(profile.id, DelimiterSide.Open)
        }
        if (profile.inlineClose.isBlank()) {
            issues += BlankInlineDelimiter(profile.id, DelimiterSide.Close)
        }
        if (profile.displayOpen.isBlank()) {
            issues += BlankDisplayDelimiter(profile.id, DelimiterSide.Open)
        }
        if (profile.displayClose.isBlank()) {
            issues += BlankDisplayDelimiter(profile.id, DelimiterSide.Close)
        }

        return if (issues.isEmpty()) {
            ValidationResult.valid(profile)
        } else {
            ValidationResult.invalid(issues)
        }
    }
}
