# DroidJax

Android helper for MathJax/TeX input.

## Module structure

- `:core` is a pure Kotlin module. It owns reusable TeX snippets, delimiter
  profiles, placeholder parsing, placeholder navigation, catalog search, and the
  platform-neutral `InsertOperation` model.
- `:android-common` is an Android library for shared Android wrappers and
  utilities. It currently contains insertion adapters and a SharedPreferences
  store for Android-owned `DroidJaxState` persistence.
- `:keyboard-ime` is an Android library for the IME frontend. It currently
  contains a compact `InputMethodService` keyboard with mode/category browsing,
  snippet keys, delimiter switching, favorites, and recents.
- `:floating-helper` is an Android library for the normal Activity-based helper
  surface. It can browse snippets by mode/category, search, save favorites,
  compose TeX, move through placeholders, persist delimiter profile changes,
  record recent snippet usage, and copy the composed text.
- `:app` is a minimal Android app shell for future settings and onboarding.

Android modules depend on `:core`; `:core` remains Android-free.

## Core ownership

The core module does not know about Android `InputConnection`, clipboard,
accessibility, overlays, or Compose. Frontends should ask core for an
`InsertOperation`, then apply it through the frontend-specific text insertion
surface.

An `InsertOperation` contains:

- `text`: clean TeX/MathJax text to insert.
- `cursorOffsetFromEnd`: where the initial cursor should land after insertion.
- `placeholderRanges`: zero-width placeholder targets in the inserted text.
- `placeholders`: richer placeholder metadata with labels, default text, and
  selection bounds.
- optional snippet metadata (`id`, `title`, `category`).

## Examples

Create a fraction insertion:

```kotlin
val fraction = SnippetCatalog
    .builtIn()
    .first { it.id == "fraction" }
    .toInsertOperation()

// fraction.text == "\\frac{}{}"
// fraction.initialCursorPosition == 6
// fraction.placeholderRanges == listOf(6..6, 8..8)
// fraction.placeholders.map { it.label } == listOf("numerator", "denominator")
```

Move through placeholders:

```kotlin
val session = PlaceholderSession.start(fraction)
val secondPlaceholder = session.next()
```

Use a delimiter profile:

```kotlin
val dollarInline = SnippetCatalog
    .builtIn(DelimiterProfile.DollarStyle)
    .first { it.id == "inline-math" }
    .toInsertOperation()

// dollarInline.text == "$$"
// dollarInline.initialCursorPosition == 1
```

Custom delimiters are plain data:

```kotlin
val profile = DelimiterProfile(
    id = "custom",
    title = "Custom",
    inlineOpen = "\\(",
    inlineClose = "\\)",
    displayOpen = "\\begin{equation}",
    displayClose = "\\end{equation}",
)
```

Validate and look up delimiter profiles:

```kotlin
val profiles = DelimiterProfileLibrary(
    userProfiles = listOf(profile),
)

val activeProfile = profiles.findById("custom")
val profileValidation = profiles.validate()
```

Validate and compose user snippets:

```kotlin
val userSnippet = UserSnippet(
    id = "quadratic-formula",
    title = "Quadratic Formula",
    templateBody = "x = \\frac{<|term=-b> \\pm \\sqrt{<radicand=b^2-4ac>}}{<denominator=2a>}",
)

val library = SnippetLibrary(userSnippets = listOf(userSnippet))
val validation = library.validate()

if (validation.isValid) {
    val matches = library.search("quadratic")
}
```

Group reusable snippets into packs:

```kotlin
val calculusPack = SnippetPack(
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
)

val library = SnippetLibrary(snippetPacks = listOf(calculusPack))
val matches = library.search("derivative")
```

Create a portable import/export payload:

```kotlin
val export = DroidJaxExport.fromState(state)
val validation = export.validate()

if (validation.isValid) {
    val importedState = export.importInto(DroidJaxState()).value
}
```

`DroidJaxExport` is a pure Kotlin model. It does not choose a wire format; UI or
sharing code can serialize it as JSON or another format later.

Compose text without Android APIs:

```kotlin
val composer = TextComposer("Area: ")
    .insert(SnippetCatalog.builtIn().first { it.id == "fraction" })

val next = composer.nextPlaceholder()
```

Use ranked search when UI needs ordering or match diagnostics:

```kotlin
val results = SnippetCatalog.rankedSearch("frac")
val firstMatch = results.first().match
```

Track favorites and recents without Android persistence:

```kotlin
val ref = SnippetRef("fraction")
val favorites = FavoriteSnippets().add(ref)
val recents = RecentSnippets().recordUse(ref, usedAt = 1)
```

Use one core-facing state object when a frontend wants the pieces composed:

```kotlin
val state = DroidJaxState()
    .withActiveDelimiterProfile(DelimiterProfile.DollarStyle.id)
    .toggleFavorite("fraction")
    .recordSnippetUse("fraction", usedAt = 1)

val snippets = state.search("frac")
val validation = state.validate()
```

Persist Android-owned state outside `:core`:

```kotlin
val store = SharedPreferencesDroidJaxStateStore(
    sharedPreferences = context.getSharedPreferences(
        SharedPreferencesDroidJaxStateStore.DefaultName,
        Context.MODE_PRIVATE,
    ),
)

val state = store.load()
val nextState = state
    .withActiveDelimiterProfile(DelimiterProfile.DollarStyle.id)
    .recordSnippetUse("fraction", usedAt = System.currentTimeMillis())

store.save(nextState)
```

The SharedPreferences store owns schema versioning for persisted Android state.
Legacy unversioned data is upgraded on load; newer unsupported data falls back
to the provided default state. It persists active delimiter profile, custom
delimiter profiles, standalone user snippets, snippet packs, favorites, and
recents.

## Tests

Run the core test suite with:

```sh
./gradlew test
```

Run a phone-friendly MathJax test page for IME testing:

```sh
make mathjax-test-server
```

Open the printed network URL on your phone while it is on the same Wi-Fi as this
machine. The page has a TeX textarea and a Render button backed by MathJax 3 from
jsDelivr, so the phone needs network access to load MathJax the first time.

The Android modules require a local Android SDK. On this machine,
`local.properties` points Gradle at `/Users/arr/Library/Android/sdk`; that file is
intentionally ignored by Git.

## Roadmap

The durable project task list lives in [`docs/ROADMAP.md`](docs/ROADMAP.md).
