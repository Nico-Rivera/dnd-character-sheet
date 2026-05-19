# D&D Character Sheet

An Android app for D&D 5e (2014) and 5.5e (2024) character sheets, with
support for both manual entry and rule-assisted automation, plus freehand
annotation over the sheet.

## Current state

**Commit 5 — boxify + hit dice + ruleset-aware layout.** Every grouped
statistic now lives in its own `SheetBox` (bordered rounded container).
Skills and saves branch on the character's ruleset: 5e keeps everything
in single boxes; 2024 puts each save in its own box (3×2 grid under the
ability scores) and splits skills by governing ability into one box per
ability. Hit dice get their own box with per-die-type spend/restore
controls and a Reset affordance that appears only when dice are spent.
The boxified layout sets up commit 6, where each box will become a
draggable, repositionable unit.

**Commit 4 — core-sheet editing.** A pencil icon in the top app bar
toggles edit mode. In edit mode the title bar tints, header fields
become tappable, an inline class editor appears, the HP and ability
blocks become tappable, and skill / save rows cycle through
NONE → HALF → PROFICIENT → EXPERTISE on tap. Plus quick HP heal/damage
buttons under the HP chip (always available, no edit-mode required) and
a tap-spam guard on navigation so back-button mashing can't softlock
into a blank screen.

**Commit 3 — first real Compose UI.** Two screens: a character list with
create/duplicate/delete, and a read-only character overview that renders
every value through the calculation engine and flags manual overrides
inline with a small dot. Single-activity Compose Navigation, M3 theme
with optional dynamic color, manual DI through an `AppContainer` held
by `DnDApplication` (Hilt later — at this scale the codegen would dwarf
the actual wiring).

**Commit 2 — Room data layer.** Adds `:core:data`: a Room database, DAO,
mapper, and a concrete `RoomCharacterRepository` that implements the
`CharacterRepository` interface declared in `:core:domain`.

**Commit 1 — scaffold + data model + calculations engine.** Modules and
package layout, the pure-Kotlin domain types, and a fully unit-tested
calculations engine.

## Module layout

```
:app           Android application (Compose). Currently a placeholder.
:core:domain   Pure-Kotlin data model (Character, ClassLevel, Weapon, …).
               No Android dependencies — could be reused on JVM/desktop.
:core:rules    Pure-Kotlin calculations engine. Stateless, fully testable.
               Depends only on :core:domain.
:core:data     Room storage + repository implementation. The only module
               that imports Android persistence APIs.
```

This split is deliberate: keeping rules and domain free of Android lets the
JVM unit tests run instantly without an emulator, and means homebrew /
campaign-tool features in the future can reuse the same engine on any JVM.

## Architecture

- **MVVM** in `:app` (added in a later commit alongside the UI).
- **Repository pattern** between `:app` and persistence — the
  [`CharacterRepository`](core/domain/src/main/java/com/dndsheet/domain/repository/CharacterRepository.kt)
  interface lives in `:core:domain` so neither the rules engine nor the UI
  has to know that storage is Room (when that lands).
- **Pure functions** for all rules calculations — no hidden state, no DI
  needed in `:core:rules`. The calculators are `object`s, not classes,
  because there is nothing to inject.
- **Manual overrides** are first-class: every calculated value can be pinned
  by the user, and each calculator exposes `isOverridden(...)` so the UI can
  flag pinned fields per spec §8.

## Build

Requires:

- JDK 17
- Android Studio Iguana (2023.2.1) or newer, OR Gradle 8.7+ on the CLI
- Android SDK with platform 34 installed (minSdk 29, targetSdk 34)

### First-time setup

The Gradle wrapper binaries (`gradlew`, `gradlew.bat`,
`gradle/wrapper/gradle-wrapper.jar`) are **not** checked in — they're
generated. You have two options:

**Option A — Android Studio (easiest):**
Open the project folder in Android Studio. It will offer to generate the
wrapper and sync the project automatically.

**Option B — CLI:**
With Gradle 8.7+ installed system-wide:

```
gradle wrapper --gradle-version 8.7
```

This creates the wrapper files. From then on, use `./gradlew` (Unix) or
`gradlew.bat` (Windows) for all builds.

### Running tests

```
gradlew.bat :core:rules:test                 # ~80 tests on the calc engine
gradlew.bat :core:data:testDebugUnitTest     # ~6 tests on the mapper round-trip
```

In Android Studio: right-click on either of `core/rules/src/test/java/com/dndsheet/rules`
or `core/data/src/test/java/com/dndsheet/data/mapper` and pick "Run Tests in …".

### Running the app

Pick an emulator or connected device from the run target dropdown and hit
Run. On the empty character list, tap "Add example character" — that
creates a Wizard 4 / Cleric 1 with a representative spread of skills,
abilities, weapons, and spells. Open it to see the calculation engine
rendered as a sheet.

### Building the APK

```
gradlew.bat :app:assembleDebug
```

Output lands in `app/build/outputs/apk/debug/`.

## What's in the engine

- **Ability modifiers** — uses `Math.floorDiv` rather than truncating
  division, so scores below 10 round the right direction.
- **Proficiency bonus** — closed-form `2 + (level - 1) / 4`.
- **Skill bonuses** — supports NONE / HALF / PROFICIENT / EXPERTISE, with
  half rounding down via `Math.floor(PB * 0.5)`.
- **Saving throws** — same prof-level treatment as skills, so homebrew
  expertise-on-saves works without engine changes.
- **Weapon attacks** — finesse picks higher of STR/DEX (tie → STR),
  ranged forces DEX, ability override forces a specific stat (Pact of
  the Blade, Monk Martial Arts), magic bonus folded into attack and damage,
  versatile two-handed dice available on request.
- **Spell save DC and attack** — per spellcasting class, since a
  multiclass character has separate rows for each.
- **Spell slots** — full multiclass table from PHB p. 165, with FULL/HALF/
  THIRD/PACT progressions. Warlock pact slots tracked on a separate table
  and never combine with the multiclass slots.
- **Passive scores** — perception, investigation, insight all use
  `10 + skill bonus`. Initiative is DEX modifier (feature modifiers will
  layer on in a later commit).

All calculators check `ManualOverrides` first, so the user's pinned value
always wins per spec §8.

## What's in the data layer (commit 2)

- **Hybrid storage schema** — the `characters` table has top-level columns
  for the fields the character-list screen needs (id, name, ruleset,
  total_level, updated_at, revision) plus a single `json` column with the
  full serialized `Character`. Listing characters scans the table without
  touching JSON; opening a character is one row read + one parse.
- **Forward compatibility** — `ignoreUnknownKeys = true` on the JSON
  instance means a character file from a future build (with new fields
  the current build doesn't know about) opens cleanly; the unknown fields
  are dropped silently.
- **`RoomCharacterRepository`** — auto-bumps `revision` and `updatedAt`
  on every save so the UI never has to remember to. Takes an optional
  `now: () -> Long` so tests can pin timestamps.
- **JSON export/import** — pretty-printed for export (intended for humans
  to read or share), compact for storage. Import always assigns a fresh
  UUID so re-importing the same backup creates a duplicate rather than
  overwriting.
- **Round-trip mapper tests** — six tests that build a character (default,
  fully populated, multiclass, with overrides), serialize it, and assert
  structural equality after deserialization. Catches any future field
  that gets added to `Character` but doesn't survive the trip.

## What's in commit 3 (UI)

- **`CharacterListScreen`** — `LazyColumn` of all saved characters.
  Each row shows name, class line ("Wizard 4 / Cleric 1"), and a relative
  last-edited timestamp. A FAB creates a blank Fighter 1; the empty state
  also offers an "example character" seeder so a fresh install has
  something to render.
- **`CharacterOverviewScreen`** — read-only sheet view.
  Header (class line, race, background, alignment, ruleset), vitals chips
  (level / proficiency bonus / initiative / HP), the six ability boxes
  in their classic 3×2 grid, all six saves, all 18 skills, and the three
  passive scores. Every numeric value comes through the calculator; every
  calculator's `isOverridden(...)` decides whether to render the pin dot.
- **Navigation** — single activity, `androidx.navigation:navigation-compose`,
  two routes (`characters`, `character/{id}`).
- **Theming** — `DnDTheme` uses M3 dynamic color on Android 12+, falling
  back to a parchment-and-ink palette. Annotation layer (commit 5) will
  read its ink color from `MaterialTheme.colorScheme` so it stays in sync
  with the active theme automatically.
- **DI** — `AppContainer` instantiated in `DnDApplication.onCreate()`.
  ViewModels grab the repository via `LocalContext.current.applicationContext`
  in a small helper. Trivial to swap for Hilt when the wiring outgrows it.

## What's in commit 4 (editing)

- **Edit mode toggle** — top-bar pencil. The title bar tints to
  `primaryContainer` while editing so it's obvious which mode you're in.
  Tap the check icon to exit.
- **Inline tap-to-edit dialogs**:
  - Identity: name, species, background, alignment (chip picker),
    ruleset (chip picker).
  - HP: a three-field dialog (current / max / temp). Confirm clamps
    `currentHp` to `max + temp` so reducing max doesn't leave you
    out of bounds.
  - Ability scores: tap a block, enter a value in 1–30.
- **Class editor section** — visible only in edit mode. Each class row
  shows `+`/`−` level buttons (clamped to 1..20 and respecting the 20
  total-level cap), a delete icon, and a summary line (level · hit die ·
  spellcasting ability). "Add class" opens a dialog that captures class
  name, optional subclass, hit die (d6/d8/d10/d12), progression
  (NONE/FULL/HALF/THIRD/PACT), and spellcasting ability when relevant.
- **Tap-to-cycle proficiencies** — in edit mode, tapping a save or
  skill row cycles its tier NONE → HALF → PROFICIENT → EXPERTISE → NONE.
  The bonus updates immediately because the row reads through the
  calculator. Cycling back to NONE removes the entry from the
  proficiency map rather than storing `NONE` explicitly.

## What's NOT in this commit

- **Weapon / spell / inventory editing** — separate page(s) in a later
  commit; the overview doesn't try to be every page.
- **Manual override editing** — long-press to pin/unpin any calculated
  value. The data layer already supports overrides; the UI to set them
  is queued.
- **Combat / spells / inventory pages** — a single overview page is
  enough to validate the architecture; the dedicated pages land later.
- **Annotation layer** (commit 5) — freehand ink over the sheet.
- **Export to PDF / image** — JSON export already exists via the
  repository, but the UI button to trigger it isn't wired yet.

See the project spec's priority order for what lands next.

## Edition handling

`Ruleset` is stored on the `Character`, not globally. Most of the math is
identical in both editions, but the engine already branches in one place:

- **Half-caster multiclass rounding** — 5e (2014) rounds Paladin/Ranger
  levels down for the multiclass caster level; 5.5e (2024) rounds them up.
  `SpellSlotCalculator.multiclassCasterLevel(classes, ruleset)` handles this.
  A Cleric 5 / Paladin 5 has caster level 7 in 5e and 8 in 5.5e — a real
  difference in the slot table that the engine respects today.

Other planned divergences (will be added in later commits):

- **Class/race/background catalog** — different content per edition,
  loaded as data.
- **Weapon mastery** — 5.5e only. `Weapon.mastery` field is already present;
  the rule that applies it lands when weapon mastery features are built.
- **Prepared spell counts** — 5.5e changed the formula for several classes.
  Will branch on `character.ruleset` in a `SpellPreparationCalculator`.
- **Background ASI placement** — handled at character-creation flow, not the engine.

## Testing philosophy

Calculations are pure functions with no hidden state. Tests in
`:core:rules` use plain JUnit 4 and a small `Fixtures` helper. Every
boundary case in the PHB table has at least one explicit test; the
multiclass spell slot table in particular has tests for Wizard 1/5/20,
mixed Cleric + Paladin under both rulesets, and Warlock combinations.

If a bug is ever found in a rules calculation, the fix is: add a failing
test first, then make it pass. The engine should never accumulate
"helper" code without test coverage.
