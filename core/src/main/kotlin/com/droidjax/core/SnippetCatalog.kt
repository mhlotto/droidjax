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
        const val Functions = "functions"
        const val Accents = "accents"
        const val Sets = "sets"
    }

    fun categories(): List<SnippetCategory> = listOf(
        SnippetCategory(Category.Delimiters, "Delimiters", 0),
        SnippetCategory(Category.Structure, "Structure", 10),
        SnippetCategory(Category.Scripts, "Scripts", 20),
        SnippetCategory(Category.Greek, "Greek", 30),
        SnippetCategory(Category.Operators, "Operators", 40),
        SnippetCategory(Category.Relations, "Relations", 50),
        SnippetCategory(Category.Arrows, "Arrows", 60),
        SnippetCategory(Category.Functions, "Functions", 70),
        SnippetCategory(Category.Accents, "Accents", 80),
        SnippetCategory(Category.Sets, "Sets", 90),
    )

    fun groupedBuiltIn(
        delimiterProfile: DelimiterProfile = DelimiterProfile.DefaultMathJax,
        snippets: List<Snippet> = builtIn(delimiterProfile),
    ): List<SnippetGroup> {
        val snippetsByCategory = snippets.groupBy { it.category }
        return categories().mapNotNull { category ->
            val categorySnippets = snippetsByCategory[category.id].orEmpty()
            if (categorySnippets.isEmpty()) {
                null
            } else {
                SnippetGroup(category, categorySnippets)
            }
        }
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
            previewText = "${delimiterProfile.inlineOpen}${delimiterProfile.inlineClose}",
            accessibilityLabel = "Inline math delimiters",
        ),
        Snippet(
            id = "display-math",
            title = "Display Math",
            category = Category.Delimiters,
            templateBody = delimiterProfile.displayTemplate(),
            aliases = listOf("display", "block", "equation"),
            previewText = "${delimiterProfile.displayOpen}${delimiterProfile.displayClose}",
            accessibilityLabel = "Display math delimiters",
        ),
        Snippet(
            id = "fraction",
            title = "Fraction",
            category = Category.Structure,
            templateBody = "\\frac{<|numerator>}{<denominator>}",
            aliases = listOf("frac", "division", "ratio"),
            previewText = "a/b",
            accessibilityLabel = "Fraction",
        ),
        Snippet(
            id = "square-root",
            title = "Square Root",
            category = Category.Structure,
            templateBody = "\\sqrt{<|radicand>}",
            aliases = listOf("sqrt", "radical"),
            previewText = "sqrt",
            accessibilityLabel = "Square root",
        ),
        Snippet(
            id = "nth-root",
            title = "nth Root",
            category = Category.Structure,
            templateBody = "\\sqrt[<|index>]{<radicand>}",
            aliases = listOf("root", "radical"),
            previewText = "nth root",
            accessibilityLabel = "Nth root",
        ),
        Snippet(
            id = "parentheses",
            title = "Parentheses",
            category = Category.Structure,
            templateBody = "\\left( <|content>\\right)",
            aliases = listOf("paren", "round brackets"),
            previewText = "( )",
            accessibilityLabel = "Parentheses",
        ),
        Snippet(
            id = "brackets",
            title = "Brackets",
            category = Category.Structure,
            templateBody = "\\left[ <|content>\\right]",
            aliases = listOf("square brackets"),
            previewText = "[ ]",
            accessibilityLabel = "Brackets",
        ),
        Snippet(
            id = "braces",
            title = "Braces",
            category = Category.Structure,
            templateBody = "\\left\\{ <|content>\\right\\}",
            aliases = listOf("curly braces", "set braces"),
            previewText = "{ }",
            accessibilityLabel = "Braces",
        ),
        Snippet(
            id = "matrix-2x2",
            title = "2x2 Matrix",
            category = Category.Structure,
            templateBody = "\\begin{matrix}<|a> & <b> \\\\ <c> & <d>\\end{matrix}",
            aliases = listOf("matrix", "2 by 2"),
            previewText = "[[a,b],[c,d]]",
            accessibilityLabel = "2 by 2 matrix",
        ),
        Snippet(
            id = "cases",
            title = "Cases",
            category = Category.Structure,
            templateBody = "\\begin{cases}<|value> & <condition>\\end{cases}",
            aliases = listOf("piecewise", "case"),
            previewText = "cases",
            accessibilityLabel = "Cases environment",
        ),
        Snippet(
            id = "aligned",
            title = "Aligned Equations",
            category = Category.Structure,
            templateBody = "\\begin{aligned}<|left> &= <right>\\end{aligned}",
            aliases = listOf("align", "equations"),
            previewText = "aligned",
            accessibilityLabel = "Aligned equations",
        ),
        Snippet(
            id = "superscript",
            title = "Superscript",
            category = Category.Scripts,
            templateBody = "^{<|exponent>}",
            aliases = listOf("power", "exponent"),
            previewText = "x^n",
            accessibilityLabel = "Superscript",
        ),
        Snippet(
            id = "subscript",
            title = "Subscript",
            category = Category.Scripts,
            templateBody = "_{<|index>}",
            aliases = listOf("index"),
            previewText = "x_i",
            accessibilityLabel = "Subscript",
        ),
        Snippet(
            id = "superscript-subscript",
            title = "Superscript + Subscript",
            category = Category.Scripts,
            templateBody = "_{<|index>}^{<exponent>}",
            aliases = listOf("limits", "sub sup", "script"),
            previewText = "x_i^n",
            accessibilityLabel = "Superscript and subscript",
        ),
    ) + greekSnippets() +
        operatorSnippets() +
        relationSnippets() +
        arrowSnippets() +
        functionSnippets() +
        accentSnippets() +
        setSnippets()

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
                snippet.previewText,
                snippet.accessibilityLabel,
            ).any { it.contains(normalizedQuery, ignoreCase = true) } ||
                snippet.aliases.any { it.contains(normalizedQuery, ignoreCase = true) }
        }
    }

    private fun greekSnippets(): List<Snippet> = listOf(
        symbol("alpha", "alpha", Category.Greek, "\\alpha", "α", aliases = listOf("greek alpha")),
        symbol("beta", "beta", Category.Greek, "\\beta", "β", aliases = listOf("greek beta")),
        symbol("gamma", "gamma", Category.Greek, "\\gamma", "γ", aliases = listOf("greek gamma")),
        symbol("delta", "delta", Category.Greek, "\\delta", "δ", aliases = listOf("greek delta")),
        symbol("epsilon", "epsilon", Category.Greek, "\\epsilon", "ε", aliases = listOf("greek epsilon")),
        symbol("theta", "theta", Category.Greek, "\\theta", "θ", aliases = listOf("greek theta")),
        symbol("lambda", "lambda", Category.Greek, "\\lambda", "λ", aliases = listOf("greek lambda")),
        symbol("mu", "mu", Category.Greek, "\\mu", "μ", aliases = listOf("greek mu")),
        symbol("pi", "pi", Category.Greek, "\\pi", "π", aliases = listOf("greek pi")),
        symbol("sigma", "sigma", Category.Greek, "\\sigma", "σ", aliases = listOf("greek sigma")),
        symbol("phi", "phi", Category.Greek, "\\phi", "φ", aliases = listOf("greek phi")),
        symbol("omega", "omega", Category.Greek, "\\omega", "ω", aliases = listOf("greek omega")),
    )

    private fun operatorSnippets(): List<Snippet> = listOf(
        symbol("sum", "sum", Category.Operators, "\\sum", "Σ", aliases = listOf("summation")),
        symbol("integral", "integral", Category.Operators, "\\int", "∫", aliases = listOf("int")),
        symbol("limit", "limit", Category.Operators, "\\lim", "lim", aliases = listOf("lim")),
        symbol("product", "product", Category.Operators, "\\prod", "∏", aliases = listOf("prod")),
        symbol("infinity", "infinity", Category.Operators, "\\infty", "∞", aliases = listOf("infty")),
        symbol("partial", "partial", Category.Operators, "\\partial", "∂"),
        symbol("nabla", "nabla", Category.Operators, "\\nabla", "∇", aliases = listOf("gradient", "del")),
        symbol("cdot", "cdot", Category.Operators, "\\cdot", "·", aliases = listOf("dot", "multiply")),
        symbol("times", "times", Category.Operators, "\\times", "×", aliases = listOf("multiply")),
    )

    private fun relationSnippets(): List<Snippet> = listOf(
        symbol("not-equal", "not equal", Category.Relations, "\\ne", "≠", aliases = listOf("neq")),
        symbol("less-or-equal", "less or equal", Category.Relations, "\\le", "≤", aliases = listOf("lte")),
        symbol("greater-or-equal", "greater or equal", Category.Relations, "\\ge", "≥", aliases = listOf("gte")),
        symbol("approximately", "approximately", Category.Relations, "\\approx", "≈", aliases = listOf("approx")),
        symbol("equivalent", "equivalent", Category.Relations, "\\equiv", "≡"),
        symbol("in", "in", Category.Relations, "\\in", "∈", aliases = listOf("element")),
        symbol("subset", "subset", Category.Relations, "\\subset", "⊂"),
        symbol("subseteq", "subseteq", Category.Relations, "\\subseteq", "⊆", aliases = listOf("subset equal")),
    )

    private fun arrowSnippets(): List<Snippet> = listOf(
        symbol("to", "to", Category.Arrows, "\\to", "→", aliases = listOf("arrow")),
        symbol("implies", "implies", Category.Arrows, "\\implies", "⇒", aliases = listOf("therefore")),
        symbol("iff", "iff", Category.Arrows, "\\iff", "⇔", aliases = listOf("if and only if")),
        symbol("left-arrow", "left arrow", Category.Arrows, "\\leftarrow", "←"),
        symbol("right-arrow", "right arrow", Category.Arrows, "\\rightarrow", "→"),
        symbol("maps-to", "maps to", Category.Arrows, "\\mapsto", "↦", aliases = listOf("mapsto")),
    )

    private fun functionSnippets(): List<Snippet> = listOf(
        function("sin", "\\sin"),
        function("cos", "\\cos"),
        function("tan", "\\tan"),
        function("log", "\\log"),
        function("ln", "\\ln"),
        function("exp", "\\exp"),
    )

    private fun accentSnippets(): List<Snippet> = listOf(
        accent("hat", "\\hat", "x̂"),
        accent("bar", "\\bar", "x̄"),
        accent("vec", "\\vec", "x⃗", aliases = listOf("vector")),
        accent("dot", "\\dot", "ẋ"),
    )

    private fun setSnippets(): List<Snippet> = listOf(
        symbol("real-numbers", "real numbers", Category.Sets, "\\mathbb{R}", "ℝ", aliases = listOf("reals", "real", "R")),
        symbol("natural-numbers", "natural numbers", Category.Sets, "\\mathbb{N}", "ℕ", aliases = listOf("naturals", "natural", "N")),
        symbol("integers", "integers", Category.Sets, "\\mathbb{Z}", "ℤ", aliases = listOf("Z")),
        symbol("rationals", "rationals", Category.Sets, "\\mathbb{Q}", "ℚ", aliases = listOf("Q")),
        symbol("complex-numbers", "complex numbers", Category.Sets, "\\mathbb{C}", "ℂ", aliases = listOf("complex", "C")),
        symbol("empty-set", "empty set", Category.Sets, "\\emptyset", "∅", aliases = listOf("empty")),
    )

    private fun function(
        id: String,
        tex: String,
    ): Snippet = Snippet(
        id = id,
        title = id,
        category = Category.Functions,
        templateBody = "$tex{<|argument=x>}",
        aliases = listOf("function"),
        previewText = "$id(x)",
        accessibilityLabel = "$id function",
    )

    private fun accent(
        id: String,
        tex: String,
        previewText: String,
        aliases: List<String> = emptyList(),
    ): Snippet = Snippet(
        id = id,
        title = id,
        category = Category.Accents,
        templateBody = "$tex{<|value=x>}",
        aliases = aliases,
        previewText = previewText,
        accessibilityLabel = "$id accent",
    )

    private fun symbol(
        id: String,
        title: String,
        category: String,
        tex: String,
        previewText: String = tex,
        aliases: List<String> = emptyList(),
    ): Snippet = Snippet(
        id = id,
        title = title,
        category = category,
        templateBody = tex,
        aliases = aliases,
        previewText = previewText,
        accessibilityLabel = title,
    )
}
