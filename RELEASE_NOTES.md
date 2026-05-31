# D&D Easy Sheet v0.1.0

First release of **D&D Easy Sheet** — an offline Android app for creating and running Dungeons & Dragons characters under both **5e (2014)** and **2024 revised (5.5e)** rules. Stats calculate automatically as you edit, the sheet layout is yours to rearrange, and a full ink layer lets you annotate the sheet like paper.

**Requirements:** Android 10 (API 29) or newer.

## Highlights

- **Automatic calculations** — ability modifiers and proficiency bonus derive automatically, with skills and saving throws supporting proficient, half-proficient (Jack of All Trades / Remarkable Athlete), and expertise tiers, rounded per the rules.
- **Spellcasting** — per-class spell save DC and spell attack modifier.
- **HP, hit dice & passives** — temporary-HP and damage handling, hit dice per class, and configurable passive Perception / Investigation / Insight (or any skill you pin).
- **5e / 5.5e per character** — pick the ruleset per character so two characters can run under different editions side by side. All 13 classes (Artificer through Wizard) ship as presets.
- **Manual overrides** — pin any calculated value to a manual number; overrides are tracked so it's clear when a stat differs from the automatic result.
- **Freehand annotations** — a Notes-style ink layer over the whole sheet: pen with adjustable color and width, whole-stroke eraser, selection, and undo/redo. Ink saves with the character.
- **Configurable layout** — rearrange sheet boxes per character and scale fonts per box; the arrangement persists.
- **Character management** — create blank or example characters, duplicate, delete, and export/import the full sheet as JSON.
- **Offline & dark mode** — fully local with no network dependency, Material 3 theming, and dynamic color on Android 12+.

## Built with

Kotlin · Jetpack Compose + Material 3 · Room · kotlinx.serialization · coroutines/Flow · MVVM + repository architecture across modular Gradle modules.

## Install

Download `dnd-easy-sheet-0.1.0.apk` below and install it on your device (you may need to allow installs from unknown sources). The source code archives are attached automatically.

## Known limitations / coming next

- Dice rolling
- Species/race database
- Weapon boxes with automatic hit and damage display
- Features and spells boxes with automatic calculations
- Spell database
- Automatic PDF sheet-background detection with computer-vision field alignment
- PDF and image export
- Homebrew / custom classes and species

## Verify your download

```
SHA256: <paste output of: Get-FileHash "app\build\outputs\apk\release\app-release.apk" -Algorithm SHA256>
```
