# android-common

Android shared wrappers and utilities.

This module depends on `:core` and keeps Android-specific APIs out of the pure
Kotlin engine. It currently contains:

- `InputConnectionInsertAdapter` for applying `InsertOperation` values to IME
  targets.
- `EditTextInsertAdapter` for applying `InsertOperation` values to local
  editable text.
- `DroidJaxStateStore` and `SharedPreferencesDroidJaxStateStore` for persisting
  Android-owned app state before feeding it back into `:core`.

The SharedPreferences store persists the active delimiter profile id, custom
delimiter profiles, standalone user snippets, snippet packs, favorite snippet
ids, and recent snippet usage. It does not persist built-in catalog data or
frontend UI state.

State preferences carry a schema version. Missing versions are treated as legacy
data and upgraded on load; newer-than-supported versions fall back to the
provided default state without overwriting the stored values.
