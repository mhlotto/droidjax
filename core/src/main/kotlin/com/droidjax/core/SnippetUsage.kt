package com.droidjax.core

data class SnippetRef(
    val id: String,
) {
    init {
        require(id.isNotBlank()) { "SnippetRef id must not be blank." }
    }

    companion object {
        fun from(snippet: Snippet): SnippetRef =
            SnippetRef(snippet.id)
    }
}

data class FavoriteSnippets(
    val refs: List<SnippetRef> = emptyList(),
) {
    fun add(ref: SnippetRef): FavoriteSnippets =
        if (ref in refs) {
            this
        } else {
            copy(refs = refs + ref)
        }

    fun remove(ref: SnippetRef): FavoriteSnippets =
        copy(refs = refs.filterNot { it == ref })

    fun toggle(ref: SnippetRef): FavoriteSnippets =
        if (ref in refs) remove(ref) else add(ref)

    fun contains(ref: SnippetRef): Boolean =
        ref in refs

    fun resolve(library: SnippetLibrary): List<Snippet> {
        val snippetsById = library.snippets.associateBy { it.id }
        return refs.mapNotNull { ref -> snippetsById[ref.id] }
    }
}

data class RecentSnippet(
    val ref: SnippetRef,
    val lastUsedAt: Long,
    val useCount: Int,
) {
    init {
        require(lastUsedAt >= 0) { "lastUsedAt must be non-negative." }
        require(useCount > 0) { "useCount must be positive." }
    }
}

data class RecentSnippets(
    val items: List<RecentSnippet> = emptyList(),
    val maxSize: Int = DefaultMaxSize,
) {
    init {
        require(maxSize > 0) { "maxSize must be positive." }
        require(items.size <= maxSize) { "items must not exceed maxSize." }
    }

    fun recordUse(
        ref: SnippetRef,
        usedAt: Long,
    ): RecentSnippets {
        val existing = items.firstOrNull { it.ref == ref }
        val updated = RecentSnippet(
            ref = ref,
            lastUsedAt = usedAt,
            useCount = (existing?.useCount ?: 0) + 1,
        )

        val newItems = (items.filterNot { it.ref == ref } + updated)
            .sortedWith(
                compareByDescending<RecentSnippet> { it.lastUsedAt }
                    .thenBy { it.ref.id },
            )
            .take(maxSize)

        return copy(items = newItems)
    }

    fun remove(ref: SnippetRef): RecentSnippets =
        copy(items = items.filterNot { it.ref == ref })

    fun clear(): RecentSnippets =
        copy(items = emptyList())

    fun resolve(library: SnippetLibrary): List<Snippet> {
        val snippetsById = library.snippets.associateBy { it.id }
        return items.mapNotNull { item -> snippetsById[item.ref.id] }
    }

    companion object {
        const val DefaultMaxSize = 20
    }
}
