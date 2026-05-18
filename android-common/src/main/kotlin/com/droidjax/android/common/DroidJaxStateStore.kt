package com.droidjax.android.common

import android.content.SharedPreferences
import com.droidjax.core.DelimiterProfile
import com.droidjax.core.DroidJaxState
import com.droidjax.core.FavoriteSnippets
import com.droidjax.core.RecentSnippet
import com.droidjax.core.RecentSnippets
import com.droidjax.core.SnippetRef
import java.net.URLDecoder
import java.net.URLEncoder

interface DroidJaxStateStore {
    fun load(): DroidJaxState

    fun save(state: DroidJaxState)
}

class SharedPreferencesDroidJaxStateStore(
    private val sharedPreferences: SharedPreferences,
    private val defaultState: DroidJaxState = DroidJaxState(),
) : DroidJaxStateStore {
    override fun load(): DroidJaxState {
        val snapshot = readSnapshot()
        val migratedSnapshot = DroidJaxStatePreferencesMigration.migrate(snapshot)

        if (snapshot.schemaVersion < DroidJaxStatePreferencesSchema.CurrentSchemaVersion) {
            writeSnapshot(migratedSnapshot)
        }

        return DroidJaxStatePreferencesCodec.toState(
            snapshot = migratedSnapshot,
            defaultState = defaultState,
        )
    }

    override fun save(state: DroidJaxState) {
        val snapshot = DroidJaxStatePreferencesCodec.toSnapshot(state)
        writeSnapshot(snapshot)
    }

    private fun readSnapshot(): DroidJaxStatePreferencesSnapshot =
        DroidJaxStatePreferencesSnapshot(
            schemaVersion = sharedPreferences.getInt(
                KeySchemaVersion,
                DroidJaxStatePreferencesSchema.LegacySchemaVersion,
            ),
            activeDelimiterProfileId = sharedPreferences.getString(
                KeyActiveDelimiterProfileId,
                null,
            ),
            favoriteSnippetIds = sharedPreferences.getString(KeyFavoriteSnippetIds, null),
            recentSnippets = sharedPreferences.getString(KeyRecentSnippets, null),
            recentMaxSize = sharedPreferences.getInt(
                KeyRecentMaxSize,
                defaultState.recents.maxSize,
            ),
            userDelimiterProfiles = sharedPreferences.getString(
                KeyUserDelimiterProfiles,
                null,
            ),
        )

    private fun writeSnapshot(snapshot: DroidJaxStatePreferencesSnapshot) {
        sharedPreferences.edit()
            .putInt(KeySchemaVersion, snapshot.schemaVersion)
            .putString(KeyActiveDelimiterProfileId, snapshot.activeDelimiterProfileId)
            .putString(KeyFavoriteSnippetIds, snapshot.favoriteSnippetIds)
            .putString(KeyRecentSnippets, snapshot.recentSnippets)
            .putInt(KeyRecentMaxSize, snapshot.recentMaxSize)
            .putString(KeyUserDelimiterProfiles, snapshot.userDelimiterProfiles)
            .apply()
    }

    companion object {
        const val DefaultName = "droidjax_state"

        private const val KeySchemaVersion = "schema_version"
        private const val KeyActiveDelimiterProfileId = "active_delimiter_profile_id"
        private const val KeyFavoriteSnippetIds = "favorite_snippet_ids"
        private const val KeyRecentSnippets = "recent_snippets"
        private const val KeyRecentMaxSize = "recent_max_size"
        private const val KeyUserDelimiterProfiles = "user_delimiter_profiles"
    }
}

internal data class DroidJaxStatePreferencesSnapshot(
    val schemaVersion: Int = DroidJaxStatePreferencesSchema.CurrentSchemaVersion,
    val activeDelimiterProfileId: String? = null,
    val favoriteSnippetIds: String? = null,
    val recentSnippets: String? = null,
    val recentMaxSize: Int = RecentSnippets.DefaultMaxSize,
    val userDelimiterProfiles: String? = null,
)

internal object DroidJaxStatePreferencesCodec {
    fun toSnapshot(state: DroidJaxState): DroidJaxStatePreferencesSnapshot =
        DroidJaxStatePreferencesSnapshot(
            schemaVersion = DroidJaxStatePreferencesSchema.CurrentSchemaVersion,
            activeDelimiterProfileId = state.activeDelimiterProfileId,
            favoriteSnippetIds = encodeList(state.favorites.refs.map { it.id }),
            recentSnippets = encodeRecentSnippets(state.recents.items),
            recentMaxSize = state.recents.maxSize,
            userDelimiterProfiles = encodeDelimiterProfiles(
                state.delimiterProfileLibrary.userProfiles,
            ),
        )

    fun toState(
        snapshot: DroidJaxStatePreferencesSnapshot,
        defaultState: DroidJaxState,
    ): DroidJaxState {
        val migratedSnapshot = DroidJaxStatePreferencesMigration.migrate(snapshot)
        val recentMaxSize = migratedSnapshot.recentMaxSize
            .takeIf { it > 0 }
            ?: defaultState.recents.maxSize
        val userProfiles = if (migratedSnapshot.userDelimiterProfiles == null) {
            defaultState.delimiterProfileLibrary.userProfiles
        } else {
            decodeDelimiterProfiles(migratedSnapshot.userDelimiterProfiles)
        }
        val favoriteRefs = if (migratedSnapshot.favoriteSnippetIds == null) {
            defaultState.favorites.refs
        } else {
            decodeList(migratedSnapshot.favoriteSnippetIds).map(::SnippetRef)
        }
        val recentItems = if (migratedSnapshot.recentSnippets == null) {
            defaultState.recents.items
        } else {
            decodeRecentSnippets(migratedSnapshot.recentSnippets, recentMaxSize)
        }

        return defaultState.copy(
            activeDelimiterProfileId = migratedSnapshot.activeDelimiterProfileId
                ?.takeIf { it.isNotBlank() }
                ?: defaultState.activeDelimiterProfileId,
            delimiterProfileLibrary = defaultState.delimiterProfileLibrary.copy(
                userProfiles = userProfiles,
            ),
            favorites = FavoriteSnippets(
                refs = favoriteRefs,
            ),
            recents = RecentSnippets(
                items = recentItems,
                maxSize = recentMaxSize,
            ),
        )
    }

    private fun encodeRecentSnippets(items: List<RecentSnippet>): String =
        items.joinToString("\n") { item ->
            listOf(
                encodeField(item.ref.id),
                item.lastUsedAt.toString(),
                item.useCount.toString(),
            ).joinToString(FieldSeparator)
        }

    private fun decodeRecentSnippets(
        value: String?,
        maxSize: Int,
    ): List<RecentSnippet> =
        value.orEmpty()
            .lineSequence()
            .mapNotNull { line ->
                val fields = line.split(FieldSeparator)
                if (fields.size != 3) {
                    null
                } else {
                    val id = decodeField(fields[0])?.takeIf { it.isNotBlank() }
                    val lastUsedAt = fields[1].toLongOrNull()
                    val useCount = fields[2].toIntOrNull()
                    if (id == null || lastUsedAt == null || lastUsedAt < 0 ||
                        useCount == null || useCount <= 0
                    ) {
                        null
                    } else {
                        RecentSnippet(
                            ref = SnippetRef(id),
                            lastUsedAt = lastUsedAt,
                            useCount = useCount,
                        )
                    }
                }
            }
            .sortedWith(
                compareByDescending<RecentSnippet> { it.lastUsedAt }
                    .thenBy { it.ref.id },
            )
            .take(maxSize)
            .toList()

    private fun encodeDelimiterProfiles(profiles: List<DelimiterProfile>): String =
        profiles.joinToString("\n") { profile ->
            listOf(
                profile.id,
                profile.title,
                profile.inlineOpen,
                profile.inlineClose,
                profile.displayOpen,
                profile.displayClose,
            ).joinToString(FieldSeparator) { encodeField(it) }
        }

    private fun decodeDelimiterProfiles(value: String?): List<DelimiterProfile> =
        value.orEmpty()
            .lineSequence()
            .mapNotNull { line ->
                val fields = line.split(FieldSeparator)
                if (fields.size != 6) {
                    null
                } else {
                    val decoded = fields.map { field -> decodeField(field) }
                    if (decoded.any { it == null }) {
                        null
                    } else {
                        DelimiterProfile(
                            id = decoded[0].orEmpty(),
                            title = decoded[1].orEmpty(),
                            inlineOpen = decoded[2].orEmpty(),
                            inlineClose = decoded[3].orEmpty(),
                            displayOpen = decoded[4].orEmpty(),
                            displayClose = decoded[5].orEmpty(),
                        )
                    }
                }
            }
            .toList()

    private fun encodeList(values: List<String>): String =
        values.joinToString("\n") { encodeField(it) }

    private fun decodeList(value: String?): List<String> =
        value.orEmpty()
            .lineSequence()
            .mapNotNull { field -> decodeField(field)?.takeIf { it.isNotBlank() } }
            .toList()

    private fun encodeField(value: String): String =
        URLEncoder.encode(value, CharsetName)

    private fun decodeField(value: String): String? =
        runCatching {
            URLDecoder.decode(value, CharsetName)
        }.getOrNull()

    private const val FieldSeparator = "\t"
    private const val CharsetName = "UTF-8"
}

internal object DroidJaxStatePreferencesMigration {
    fun migrate(snapshot: DroidJaxStatePreferencesSnapshot): DroidJaxStatePreferencesSnapshot =
        when {
            snapshot.schemaVersion <= DroidJaxStatePreferencesSchema.LegacySchemaVersion ->
                snapshot.copy(schemaVersion = DroidJaxStatePreferencesSchema.CurrentSchemaVersion)

            snapshot.schemaVersion > DroidJaxStatePreferencesSchema.CurrentSchemaVersion ->
                DroidJaxStatePreferencesSnapshot()

            else -> snapshot
        }
}

internal object DroidJaxStatePreferencesSchema {
    const val LegacySchemaVersion = 0
    const val CurrentSchemaVersion = 1
}
