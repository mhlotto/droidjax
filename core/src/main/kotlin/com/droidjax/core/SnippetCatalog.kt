package com.droidjax.core

object SnippetCatalog {
    object Category {
        const val Delimiters = "delimiters"
        const val Structure = "structure"
        const val Scripts = "scripts"
        const val Greek = "greek"
        const val Operators = "operators"
        const val Relations = "relations"
        const val Arrows = "arrows"
    }

    fun builtIn(
        delimiterProfile: DelimiterProfile = DelimiterProfile.DefaultMathJax,
    ): List<Snippet> = listOf(
        Snippet(
            id = "inline-math",
            title = "Inline Math",
            category = Category.Delimiters,
            templateBody = delimiterProfile.inlineTemplate(),
            aliases = listOf("inline", "mathjax", "math"),
        ),
        Snippet(
            id = "display-math",
            title = "Display Math",
            category = Category.Delimiters,
            templateBody = delimiterProfile.displayTemplate(),
            aliases = listOf("display", "block", "equation"),
        ),
        Snippet(
            id = "fraction",
            title = "Fraction",
            category = Category.Structure,
            templateBody = "\\frac{<|>}{<>}",
            aliases = listOf("frac", "division", "ratio"),
        ),
        Snippet(
            id = "square-root",
            title = "Square Root",
            category = Category.Structure,
            templateBody = "\\sqrt{<|>}",
            aliases = listOf("sqrt", "radical"),
        ),
        Snippet(
            id = "nth-root",
            title = "nth Root",
            category = Category.Structure,
            templateBody = "\\sqrt[<|>]{<>}",
            aliases = listOf("root", "radical"),
        ),
        Snippet(
            id = "parentheses",
            title = "Parentheses",
            category = Category.Structure,
            templateBody = "\\left( <|>\\right)",
            aliases = listOf("paren", "round brackets"),
        ),
        Snippet(
            id = "brackets",
            title = "Brackets",
            category = Category.Structure,
            templateBody = "\\left[ <|>\\right]",
            aliases = listOf("square brackets"),
        ),
        Snippet(
            id = "braces",
            title = "Braces",
            category = Category.Structure,
            templateBody = "\\left\\{ <|>\\right\\}",
            aliases = listOf("curly braces", "set braces"),
        ),
        Snippet(
            id = "superscript",
            title = "Superscript",
            category = Category.Scripts,
            templateBody = "^{<|>}",
            aliases = listOf("power", "exponent"),
        ),
        Snippet(
            id = "subscript",
            title = "Subscript",
            category = Category.Scripts,
            templateBody = "_{<|>}",
            aliases = listOf("index"),
        ),
        Snippet(
            id = "superscript-subscript",
            title = "Superscript + Subscript",
            category = Category.Scripts,
            templateBody = "_{<|>}^{<>}",
            aliases = listOf("limits", "sub sup", "script"),
        ),
    ) + greekSnippets() + operatorSnippets() + relationSnippets() + arrowSnippets()

    fun search(
        query: String,
        snippets: List<Snippet> = builtIn(),
    ): List<Snippet> {
        val normalizedQuery = query.trim().lowercase()
        if (normalizedQuery.isEmpty()) return snippets

        return snippets.filter { snippet ->
            listOf(
                snippet.id,
                snippet.title,
                snippet.templateBody,
            ).any { it.contains(normalizedQuery, ignoreCase = true) } ||
                snippet.aliases.any { it.contains(normalizedQuery, ignoreCase = true) }
        }
    }

    private fun greekSnippets(): List<Snippet> = listOf(
        symbol("alpha", "alpha", Category.Greek, "\\alpha", aliases = listOf("greek alpha")),
        symbol("beta", "beta", Category.Greek, "\\beta", aliases = listOf("greek beta")),
        symbol("gamma", "gamma", Category.Greek, "\\gamma", aliases = listOf("greek gamma")),
        symbol("delta", "delta", Category.Greek, "\\delta", aliases = listOf("greek delta")),
        symbol("epsilon", "epsilon", Category.Greek, "\\epsilon", aliases = listOf("greek epsilon")),
        symbol("theta", "theta", Category.Greek, "\\theta", aliases = listOf("greek theta")),
        symbol("lambda", "lambda", Category.Greek, "\\lambda", aliases = listOf("greek lambda")),
        symbol("mu", "mu", Category.Greek, "\\mu", aliases = listOf("greek mu")),
        symbol("pi", "pi", Category.Greek, "\\pi", aliases = listOf("greek pi")),
        symbol("sigma", "sigma", Category.Greek, "\\sigma", aliases = listOf("greek sigma")),
        symbol("phi", "phi", Category.Greek, "\\phi", aliases = listOf("greek phi")),
        symbol("omega", "omega", Category.Greek, "\\omega", aliases = listOf("greek omega")),
    )

    private fun operatorSnippets(): List<Snippet> = listOf(
        symbol("sum", "sum", Category.Operators, "\\sum", aliases = listOf("summation")),
        symbol("integral", "integral", Category.Operators, "\\int", aliases = listOf("int")),
        symbol("limit", "limit", Category.Operators, "\\lim", aliases = listOf("lim")),
        symbol("product", "product", Category.Operators, "\\prod", aliases = listOf("prod")),
        symbol("infinity", "infinity", Category.Operators, "\\infty", aliases = listOf("infty")),
        symbol("partial", "partial", Category.Operators, "\\partial"),
        symbol("nabla", "nabla", Category.Operators, "\\nabla", aliases = listOf("gradient", "del")),
        symbol("cdot", "cdot", Category.Operators, "\\cdot", aliases = listOf("dot", "multiply")),
        symbol("times", "times", Category.Operators, "\\times", aliases = listOf("multiply")),
    )

    private fun relationSnippets(): List<Snippet> = listOf(
        symbol("not-equal", "not equal", Category.Relations, "\\ne", aliases = listOf("neq")),
        symbol("less-or-equal", "less or equal", Category.Relations, "\\le", aliases = listOf("lte")),
        symbol("greater-or-equal", "greater or equal", Category.Relations, "\\ge", aliases = listOf("gte")),
        symbol("approximately", "approximately", Category.Relations, "\\approx", aliases = listOf("approx")),
        symbol("equivalent", "equivalent", Category.Relations, "\\equiv"),
        symbol("in", "in", Category.Relations, "\\in", aliases = listOf("element")),
        symbol("subset", "subset", Category.Relations, "\\subset"),
        symbol("subseteq", "subseteq", Category.Relations, "\\subseteq", aliases = listOf("subset equal")),
    )

    private fun arrowSnippets(): List<Snippet> = listOf(
        symbol("to", "to", Category.Arrows, "\\to", aliases = listOf("arrow")),
        symbol("implies", "implies", Category.Arrows, "\\implies", aliases = listOf("therefore")),
        symbol("iff", "iff", Category.Arrows, "\\iff", aliases = listOf("if and only if")),
        symbol("left-arrow", "left arrow", Category.Arrows, "\\leftarrow"),
        symbol("right-arrow", "right arrow", Category.Arrows, "\\rightarrow"),
        symbol("maps-to", "maps to", Category.Arrows, "\\mapsto", aliases = listOf("mapsto")),
    )

    private fun symbol(
        id: String,
        title: String,
        category: String,
        tex: String,
        aliases: List<String> = emptyList(),
    ): Snippet = Snippet(
        id = id,
        title = title,
        category = category,
        templateBody = tex,
        aliases = aliases,
    )
}
