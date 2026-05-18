package com.droidjax.android.common

import com.droidjax.core.DelimiterProfile
import com.droidjax.core.DelimiterProfileLibrary
import com.droidjax.core.DroidJaxState
import com.droidjax.core.FavoriteSnippets
import com.droidjax.core.RecentSnippet
import com.droidjax.core.RecentSnippets
import com.droidjax.core.SnippetRef
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
            ),
            defaultState = DroidJaxState(),
        )

        assertEquals(DelimiterProfile.DefaultMathJax.id, restored.activeDelimiterProfileId)
        assertTrue(restored.delimiterProfileLibrary.userProfiles.isEmpty())
        assertTrue(restored.favorites.refs.isEmpty())
        assertTrue(restored.recents.items.isEmpty())
        assertEquals(RecentSnippets.DefaultMaxSize, restored.recents.maxSize)
    }
}
