package com.droidjax.android.common

import android.content.SharedPreferences
import com.droidjax.core.DelimiterProfile
import com.droidjax.core.DroidJaxState
import com.droidjax.core.FavoriteSnippets
import com.droidjax.core.RecentSnippet
import com.droidjax.core.RecentSnippets
import com.droidjax.core.SnippetRef
import com.droidjax.core.SnippetPack
import com.droidjax.core.UserSnippet
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
            userSnippets = sharedPreferences.getString(KeyUserSnippets, null),
            snippetPacks = sharedPreferences.getString(KeySnippetPacks, null),
        )

    private fun writeSnapshot(snapshot: DroidJaxStatePreferencesSnapshot) {
        sharedPreferences.edit()
            .putInt(KeySchemaVersion, snapshot.schemaVersion)
            .putString(KeyActiveDelimiterProfileId, snapshot.activeDelimiterProfileId)
            .putString(KeyFavoriteSnippetIds, snapshot.favoriteSnippetIds)
            .putString(KeyRecentSnippets, snapshot.recentSnippets)
            .putInt(KeyRecentMaxSize, snapshot.recentMaxSize)
            .putString(KeyUserDelimiterProfiles, snapshot.userDelimiterProfiles)
            .putString(KeyUserSnippets, snapshot.userSnippets)
            .putString(KeySnippetPacks, snapshot.snippetPacks)
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
        private const val KeyUserSnippets = "user_snippets"
        private const val KeySnippetPacks = "snippet_packs"
    }
}

internal data class DroidJaxStatePreferencesSnapshot(
    val schemaVersion: Int = DroidJaxStatePreferencesSchema.CurrentSchemaVersion,
    val activeDelimiterProfileId: String? = null,
    val favoriteSnippetIds: String? = null,
    val recentSnippets: String? = null,
    val recentMaxSize: Int = RecentSnippets.DefaultMaxSize,
    val userDelimiterProfiles: String? = null,
    val userSnippets: String? = null,
    val snippetPacks: String? = null,
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
            userSnippets = encodeUserSnippets(
                state.snippetLibrary.userSnippets,
            ),
            snippetPacks = encodeSnippetPacks(
                state.snippetLibrary.snippetPacks,
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
        val userSnippets = if (migratedSnapshot.userSnippets == null) {
            defaultState.snippetLibrary.userSnippets
        } else {
            decodeUserSnippets(migratedSnapshot.userSnippets)
        }
        val snippetPacks = if (migratedSnapshot.snippetPacks == null) {
            defaultState.snippetLibrary.snippetPacks
        } else {
            decodeSnippetPacks(migratedSnapshot.snippetPacks)
        }

        return defaultState.copy(
            activeDelimiterProfileId = migratedSnapshot.activeDelimiterProfileId
                ?.takeIf { it.isNotBlank() }
                ?: defaultState.activeDelimiterProfileId,
            snippetLibrary = defaultState.snippetLibrary.copy(
                userSnippets = userSnippets,
                snippetPacks = snippetPacks,
            ),
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

    private fun encodeUserSnippets(snippets: List<UserSnippet>): String =
        snippets.joinToString("\n", transform = ::encodeUserSnippet)

    private fun decodeUserSnippets(value: String?): List<UserSnippet> =
        value.orEmpty()
            .lineSequence()
            .mapNotNull(::decodeUserSnippet)
            .toList()

    private fun encodeUserSnippet(snippet: UserSnippet): String =
        listOf(
            encodeField(snippet.id),
            encodeField(snippet.title),
            encodeField(snippet.category),
            encodeField(snippet.templateBody),
            encodeStringList(snippet.aliases),
            encodeField(snippet.previewText),
            encodeField(snippet.accessibilityLabel),
        ).joinToString(FieldSeparator)

    private fun decodeUserSnippet(line: String): UserSnippet? {
        val fields = line.split(FieldSeparator)
        if (fields.size != UserSnippetFieldCount) return null

        val id = decodeField(fields[0])?.takeIf { it.isNotBlank() } ?: return null
        val title = decodeField(fields[1]) ?: return null
        val category = decodeField(fields[2]) ?: return null
        val templateBody = decodeField(fields[3]) ?: return null
        val aliases = decodeStringList(fields[4])
        val previewText = decodeField(fields[5]) ?: return null
        val accessibilityLabel = decodeField(fields[6]) ?: return null

        return UserSnippet(
            id = id,
            title = title,
            category = category,
            templateBody = templateBody,
            aliases = aliases,
            previewText = previewText,
            accessibilityLabel = accessibilityLabel,
        )
    }

    private fun encodeSnippetPacks(packs: List<SnippetPack>): String =
        packs.joinToString("\n") { pack ->
            listOf(
                encodeField(pack.id),
                encodeField(pack.title),
                encodeField(encodeUserSnippets(pack.snippets)),
            ).joinToString(FieldSeparator)
        }

    private fun decodeSnippetPacks(value: String?): List<SnippetPack> =
        value.orEmpty()
            .lineSequence()
            .mapNotNull { line ->
                val fields = line.split(FieldSeparator)
                if (fields.size != SnippetPackFieldCount) {
                    null
                } else {
                    val id = decodeField(fields[0])?.takeIf { it.isNotBlank() }
                    val title = decodeField(fields[1])
                    val snippets = decodeField(fields[2])?.let(::decodeUserSnippets)
                    if (id == null || title == null || snippets == null) {
                        null
                    } else {
                        SnippetPack(
                            id = id,
                            title = title,
                            snippets = snippets,
                        )
                    }
                }
            }
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

    private fun encodeStringList(values: List<String>): String =
        values.joinToString(ListSeparator) { encodeField(it) }

    private fun decodeStringList(value: String): List<String> =
        value.split(ListSeparator)
            .filter { it.isNotBlank() }
            .mapNotNull(::decodeField)

    private fun encodeField(value: String): String =
        URLEncoder.encode(value, CharsetName)

    private fun decodeField(value: String): String? =
        runCatching {
            URLDecoder.decode(value, CharsetName)
        }.getOrNull()

    private const val FieldSeparator = "\t"
    private const val ListSeparator = "\u001F"
    private const val CharsetName = "UTF-8"
    private const val UserSnippetFieldCount = 7
    private const val SnippetPackFieldCount = 3
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
    const val CurrentSchemaVersion = 2
}
