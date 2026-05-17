package com.droidjax.core

data class DelimiterProfile(
    val id: String,
    val title: String,
    val inlineOpen: String,
    val inlineClose: String,
    val displayOpen: String,
    val displayClose: String,
) {
    fun inlineTemplate(): String = "$inlineOpen${TemplateEngine.INITIAL_PLACEHOLDER}$inlineClose"

    fun displayTemplate(): String = "$displayOpen${TemplateEngine.INITIAL_PLACEHOLDER}$displayClose"

    companion object {
        val DefaultMathJax = DelimiterProfile(
            id = "default-mathjax",
            title = "Default MathJax",
            inlineOpen = "\\(",
            inlineClose = "\\)",
            displayOpen = "\\[",
            displayClose = "\\]",
        )

        val DollarStyle = DelimiterProfile(
            id = "dollar-style",
            title = "Dollar Style",
            inlineOpen = "$",
            inlineClose = "$",
            displayOpen = "$$",
            displayClose = "$$",
        )
    }
}
