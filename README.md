# DroidJax

Android helper for MathJax/TeX input.

## Module structure

- `:core` is a pure Kotlin module. It owns reusable TeX snippets, delimiter
  profiles, placeholder parsing, placeholder navigation, catalog search, and the
  platform-neutral `InsertOperation` model.
- `:android-common` is an Android library for shared Android wrappers and
  utilities. It currently contains an `InputConnection` adapter for
  `InsertOperation`.
- `:keyboard-ime` is an Android library for the IME frontend. It currently
  contains a minimal `InputMethodService` proof of concept.
- `:floating-helper` is an Android library for the normal Activity-based helper
  prototype. It can search grouped snippets, compose TeX, move through
  placeholders, switch delimiter profiles in memory, and copy the composed text.
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

## Tests

Run the core test suite with:

```sh
./gradlew test
```

The Android modules require a local Android SDK. On this machine,
`local.properties` points Gradle at `/Users/arr/Library/Android/sdk`; that file is
intentionally ignored by Git.

## Roadmap

The durable project task list lives in [`docs/ROADMAP.md`](docs/ROADMAP.md).
