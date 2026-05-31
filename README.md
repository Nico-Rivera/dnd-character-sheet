# D&D Easy Sheet App

An offline-first Android app for creating and running **Dungeons & Dragons** characters under both the **5e (2014)** and **2024 revised (5.5e)** rules. Stats are calculated automatically as you edit, the sheet layout is yours to rearrange, and a full ink layer lets you scribble on top of it like a paper sheet.

> **Release:** `v0.1.0` · `minSdk 29` (Android 10+) · `targetSdk / compileSdk 34`

## Screenshots

| Character list | Sheet overview | Ink mode |
| :---: | :---: | :---: |
| _add `docs/screenshots/list.png`_ | _add `docs/screenshots/sheet.png`_ | _add `docs/screenshots/ink.png`_ |

> Screenshots are placeholders — drop images into `docs/screenshots/` and the table above will render them.

## Features

- **Automatic modifier calculations** — ability modifiers use the standard `floor((score − 10) / 2)` formula, and proficiency bonus is derived from total character level across all classes.
- **Skills & saving throws** — bonuses recompute from the governing ability plus proficiency tier, with full support for proficient, half-proficient (Jack of All Trades / Remarkable Athlete), and expertise, all rounded per the rules.
- **Weapons** — attack and damage bonuses pick the right ability automatically, handling finesse (best of STR/DEX), ranged, versatile damage, magic bonuses, proficiency inclusion, and per-weapon ability overrides (Pact of the Blade, Martial Arts).
- **Spellcasting** — per-class spell save DC and spell attack modifier, plus multiclass spell-slot tables and Warlock pact magic. Half-caster slot rounding follows the edition (5e floors, 5.5e ceilings).
- **HP, hit dice & passives** — temporary-HP and damage rules, hit dice per class, and configurable passive Perception / Investigation / Insight (or any skill you pin).
- **D&D 5e / 5.5e support** — choose the ruleset per character, so two characters in one campaign can run under different editions side by side. All 13 classes (Artificer through Wizard) ship as presets.
- **Manual overrides** — any calculated value can be pinned to a manual number; overridden values are tracked separately so it's clear when a stat differs from the automatic result.
- **Freehand annotations** — a Samsung Notes-style ink layer sits on top of the whole sheet: pen with adjustable color and width, whole-stroke eraser, selection, and 50-step undo/redo. Ink is saved with the character.
- **Configurable layout** — rearrange the sheet's boxes per character and scale fonts per box; the arrangement persists.
- **Character management** — create blank or example characters, duplicate, delete, and round-trip the full sheet to/from JSON for export/import.
- **Offline-first & dark mode** — everything runs locally with no network dependency, with Material 3 theming and dynamic color on Android 12+.

## Tech Stack

- **Kotlin**
- **Jetpack Compose** + **Material 3** (single-Activity UI, Navigation Compose)
- **Room** for local persistence (KSP annotation processor)
- **kotlinx.serialization** for JSON encoding of the domain model
- **kotlinx.coroutines** + **Flow** for reactive, autosaving state
- **ViewModel** / Lifecycle Compose integration
- **JUnit** for the calculation test suite

## Architecture

The project follows an **MVVM + repository** pattern split across Gradle modules:

- **`:core:domain`** — pure-Kotlin (no Android) domain model. `Character` is the aggregate root; everything the engine needs is reachable from it. All models are `@Serializable`. Defines the `CharacterRepository` contract.
- **`:core:rules`** — stateless calculators (`AbilityCalculator`, `SkillCalculator`, `SavingThrowCalculator`, `ProficiencyCalculator`, `WeaponCalculator`, `SpellcastingCalculator`, `SpellSlotCalculator`, `HpCalculator`, `PassiveCalculator`). Each is independently unit-tested.
- **`:core:data`** — Room implementation of the repository. A hybrid schema keeps list-screen columns (id, name, ruleset, level, revision) queryable while storing the full character as a JSON blob, so most model changes need no migration.
- **`:app`** — Compose UI, ViewModels, navigation, and the ink/layout layer.

Each save bumps a monotonic `revision`, and the UI observes character data via `Flow` so changes appear reactively.

## Getting Started

1. **Clone** the repository:
   ```bash
   git clone <repo-url>
   cd dnd-character-sheet
   ```
2. **Open in Android Studio** (a recent Hedgehog+ build with the Android SDK for API 34 and JDK 17).
3. **Sync Gradle** — let the IDE resolve dependencies and the KSP plugin.
4. **Run** the `:app` configuration on an emulator or device running Android 10 (API 29) or newer.

## Roadmap

- Dice rolling and initiative tracking
- 5.5e weapon mastery rules (stored on weapons today, not yet applied to math)
- PDF sheet backgrounds with CV-detected field alignment (model field already present)
- PDF and image export (JSON export ships today)
- Homebrew / custom classes and species
- Spell database and monster stat-block integration
- Campaign management, shared party sheets, and cloud sync

## Contributing

Contributions are welcome. Keep changes small and focused, preserve the module boundaries, and add tests in `:core:rules` for any calculation change. Open an issue to discuss larger features before starting.

## License

See [`LICENSE`](LICENSE) for the full text.
