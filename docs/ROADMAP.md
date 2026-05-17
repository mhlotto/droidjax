# DroidJax Roadmap

This is the durable working list for the project. Keep it current as the codebase
evolves so future sessions can pick up the intended direction without relying on
chat history.

## Foundation

- [x] Create a pure Kotlin `:core` module.
- [x] Define platform-neutral `InsertOperation`.
- [x] Define snippets/templates and delimiter profiles.
- [x] Implement placeholder parsing and navigation.
- [x] Add a built-in snippet catalog and search.
- [x] Add first-pass documentation and core tests.
- [x] Convert core tests to normal JUnit tests.

## Android Project Structure

- [x] Wire `:android-common` as an Android library module.
- [x] Wire `:keyboard-ime` as an Android library module.
- [x] Wire `:floating-helper` as an Android library module.
- [x] Wire `:app` as an Android application shell.
- [x] Keep Android APIs out of `:core`.

## Android Integration

- [x] Add an Android adapter that applies `InsertOperation` to `InputConnection`.
- [x] Add a minimal `InputMethodService` proof of concept.
- [x] Add a minimal app/settings shell.
- [x] Add a normal Activity-based floating helper prototype before overlay or
  accessibility behavior.

## Core Expansion

- [x] Add snippet grouping models for UI display order and labels.
- [x] Add user-facing symbol metadata such as preview text and accessibility
  labels.
- [x] Decide whether placeholders need labels or default selected text.
- [x] Add matrices, cases, aligned equations, functions, accents, and common
  sets.
- [x] Add a pure Kotlin placeholder session model for frontend cursor movement.
- [x] Add user-defined snippet and snippet pack models.
- [x] Add pure Kotlin snippet/template/catalog validation.
- [x] Add catalog composition for built-ins plus user snippets.
- [x] Add a pure Kotlin text composer for shared insertion and placeholder movement.
- [x] Add ranked catalog search with match metadata.
- [x] Add recent snippets and favorites models.

## Settings And Persistence

- [x] Add delimiter profile validation and library models for built-ins plus custom profiles.
- [ ] Add settings models for active delimiter profile and catalog preferences.
- [ ] Persist settings in Android modules, not `:core`.
- [ ] Feed persisted settings into `:core` when building catalogs and operations.

## Verification

- [x] Keep `./gradlew test` green.
- [ ] Add Android unit tests for insertion adapters.
- [ ] Add instrumentation tests once the IME and floating helper have real UI.
