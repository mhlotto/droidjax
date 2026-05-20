package com.droidjax.android.common

import com.droidjax.core.DelimiterProfile
import com.droidjax.core.DelimiterProfileLibrary
import com.droidjax.core.DroidJaxState
import com.droidjax.core.FavoriteSnippets
import com.droidjax.core.RecentSnippet
import com.droidjax.core.RecentSnippets
import com.droidjax.core.SnippetLibrary
import com.droidjax.core.SnippetPack
import com.droidjax.core.SnippetRef
import com.droidjax.core.UserSnippet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DroidJaxStatePreferencesCodecTest {
    @Test
    fun `snapshot round trip keeps persisted state`() {
        val customProfile = DelimiterProfile(
            id = "custom-forum",
            title = "Custom Forum",
            inlineOpen = "\\(",
            inlineClose = "\\)",
            displayOpen = "\\begin{equation}",
            displayClose = "\\end{equation}",
        )
        val state = DroidJaxState(
            snippetLibrary = SnippetLibrary(
                userSnippets = listOf(
                    UserSnippet(
                        id = "quadratic-formula",
                        title = "Quadratic Formula",
                        templateBody = "x = \\frac{<|term=-b>}{<denominator=2a>}",
                        aliases = listOf("quadratic", "formula"),
                        previewText = "quadratic",
                        accessibilityLabel = "Quadratic formula",
                    ),
                ),
                snippetPacks = listOf(
                    SnippetPack(
                        id = "calculus",
                        title = "Calculus",
                        snippets = listOf(
                            UserSnippet(
                                id = "custom-derivative",
                                title = "Derivative",
                                templateBody = "\\frac{d}{d<|variable=x>} <expression=f>",
                                aliases = listOf("derivative"),
                            ),
                        ),
                    ),
                ),
            ),
            delimiterProfileLibrary = DelimiterProfileLibrary(
                userProfiles = listOf(customProfile),
            ),
            activeDelimiterProfileId = customProfile.id,
            favorites = FavoriteSnippets(
                refs = listOf(
                    SnippetRef("fraction"),
                    SnippetRef("inline-math"),
                ),
            ),
            recents = RecentSnippets(
                items = listOf(
                    RecentSnippet(
                        ref = SnippetRef("sum"),
                        lastUsedAt = 20,
                        useCount = 3,
                    ),
                    RecentSnippet(
                        ref = SnippetRef("fraction"),
                        lastUsedAt = 10,
                        useCount = 1,
                    ),
                ),
                maxSize = 5,
            ),
        )

        val snapshot = DroidJaxStatePreferencesCodec.toSnapshot(state)
        val restored = DroidJaxStatePreferencesCodec.toState(
            snapshot = snapshot,
            defaultState = DroidJaxState(),
        )

        assertEquals(DroidJaxStatePreferencesSchema.CurrentSchemaVersion, snapshot.schemaVersion)
        assertEquals(
            listOf("quadratic-formula"),
            restored.snippetLibrary.userSnippets.map { it.id },
        )
        assertEquals("quadratic", restored.snippetLibrary.userSnippets.single().previewText)
        assertEquals(
            listOf("calculus"),
            restored.snippetLibrary.snippetPacks.map { it.id },
        )
        assertEquals(
            listOf("custom-derivative"),
            restored.snippetLibrary.snippetPacks.single().snippets.map { it.id },
        )
        assertEquals(
            listOf("quadratic-formula", "custom-derivative"),
            restored.snippetLibrary.allUserSnippets.map { it.id },
        )
        assertEquals(customProfile.id, restored.activeDelimiterProfileId)
        assertEquals(customProfile, restored.delimiterProfileLibrary.userProfiles.single())
        assertEquals(
            listOf("fraction", "inline-math"),
            restored.favorites.refs.map { it.id },
        )
        assertEquals(5, restored.recents.maxSize)
        assertEquals(
            listOf("sum", "fraction"),
            restored.recents.items.map { it.ref.id },
        )
        assertEquals(listOf(3, 1), restored.recents.items.map { it.useCount })
    }

    @Test
    fun `missing persisted values preserve default state`() {
        val defaultState = DroidJaxState(
            snippetLibrary = SnippetLibrary(
                userSnippets = listOf(
                    UserSnippet(
                        id = "default-snippet",
                        title = "Default Snippet",
                        templateBody = "<|value>",
                    ),
                ),
                snippetPacks = listOf(
                    SnippetPack(
                        id = "default-pack",
                        title = "Default Pack",
                        snippets = listOf(
                            UserSnippet(
                                id = "default-pack-snippet",
                                title = "Default Pack Snippet",
                                templateBody = "<|value>",
                            ),
                        ),
                    ),
                ),
            ),
            activeDelimiterProfileId = DelimiterProfile.DollarStyle.id,
            favorites = FavoriteSnippets(
                refs = listOf(SnippetRef("fraction")),
            ),
            recents = RecentSnippets(
                items = listOf(
                    RecentSnippet(
                        ref = SnippetRef("sqrt"),
                        lastUsedAt = 30,
                        useCount = 2,
                    ),
                ),
                maxSize = 4,
            ),
        )

        val restored = DroidJaxStatePreferencesCodec.toState(
            snapshot = DroidJaxStatePreferencesSnapshot(
                recentMaxSize = defaultState.recents.maxSize,
            ),
            defaultState = defaultState,
        )

        assertEquals(defaultState.activeDelimiterProfileId, restored.activeDelimiterProfileId)
        assertEquals(defaultState.snippetLibrary, restored.snippetLibrary)
        assertEquals(defaultState.favorites, restored.favorites)
        assertEquals(defaultState.recents, restored.recents)
    }

    @Test
    fun `legacy snapshot migrates to current schema`() {
        val state = DroidJaxState(
            activeDelimiterProfileId = DelimiterProfile.DollarStyle.id,
            favorites = FavoriteSnippets(
                refs = listOf(SnippetRef("fraction")),
            ),
        )
        val legacySnapshot = DroidJaxStatePreferencesCodec.toSnapshot(state)
            .copy(schemaVersion = DroidJaxStatePreferencesSchema.LegacySchemaVersion)

        val migratedSnapshot = DroidJaxStatePreferencesMigration.migrate(legacySnapshot)
        val restored = DroidJaxStatePreferencesCodec.toState(
            snapshot = legacySnapshot,
            defaultState = DroidJaxState(),
        )

        assertEquals(
            DroidJaxStatePreferencesSchema.CurrentSchemaVersion,
            migratedSnapshot.schemaVersion,
        )
        assertEquals(DelimiterProfile.DollarStyle.id, restored.activeDelimiterProfileId)
        assertEquals(listOf("fraction"), restored.favorites.refs.map { it.id })
    }

    @Test
    fun `future snapshot version falls back to default state`() {
        val defaultState = DroidJaxState(
            activeDelimiterProfileId = DelimiterProfile.DollarStyle.id,
            favorites = FavoriteSnippets(
                refs = listOf(SnippetRef("sqrt")),
            ),
        )
        val restored = DroidJaxStatePreferencesCodec.toState(
            snapshot = DroidJaxStatePreferencesSnapshot(
                schemaVersion = DroidJaxStatePreferencesSchema.CurrentSchemaVersion + 1,
                activeDelimiterProfileId = "unknown-future-profile",
                favoriteSnippetIds = "fraction",
            ),
            defaultState = defaultState,
        )

        assertEquals(defaultState.activeDelimiterProfileId, restored.activeDelimiterProfileId)
        assertEquals(defaultState.favorites, restored.favorites)
    }

    @Test
    fun `malformed persisted values fall back or are skipped`() {
        val restored = DroidJaxStatePreferencesCodec.toState(
            snapshot = DroidJaxStatePreferencesSnapshot(
                activeDelimiterProfileId = "",
                favoriteSnippetIds = "bad%zz",
                recentSnippets = "not\tvalid\tdata",
                recentMaxSize = -1,
                userDelimiterProfiles = "bad-profile",
                userSnippets = "bad-snippet",
                snippetPacks = "bad-pack",
            ),
            defaultState = DroidJaxState(),
        )

        assertTrue(restored.snippetLibrary.userSnippets.isEmpty())
        assertTrue(restored.snippetLibrary.snippetPacks.isEmpty())
        assertEquals(DelimiterProfile.DefaultMathJax.id, restored.activeDelimiterProfileId)
        assertTrue(restored.delimiterProfileLibrary.userProfiles.isEmpty())
        assertTrue(restored.favorites.refs.isEmpty())
        assertTrue(restored.recents.items.isEmpty())
        assertEquals(RecentSnippets.DefaultMaxSize, restored.recents.maxSize)
    }
}
