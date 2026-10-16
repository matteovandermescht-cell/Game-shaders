# Game Shaders

A lightweight Android visual-overlay companion for games, built with Kotlin.

## Features
- Dark interface with visual preset selection.
- Optional transparent screen overlay (requires Android display-over-other-apps permission).
- Neutral, Night, Warm and Contrast presets.
- Non-touchable overlay designed not to block game controls.
- GitHub Actions builds a debug APK and uploads it as an artifact.

## Limitations
Android apps cannot universally replace a game's internal shaders. This project applies a transparent system overlay; results vary by game and Android version. It does not automate gameplay or target other players.

## Build
Open in Android Studio or run the GitHub Actions workflow on the main branch. The workflow uploads the debug APK as an artifact.
