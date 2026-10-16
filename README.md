# Game Shaders AI

An Android prototype for teaching a gameplay assistant from examples.

## Current features
- Choose a gameplay screenshot from the device.
- Describe what the AI should recognize in that screenshot.
- Write the desired next action and optional conditions/notes.
- Save examples locally, review them, and delete them.
- Export the examples as a JSON file for use in a future training/inference pipeline.
- GitHub Actions builds a debug APK artifact.

## Important limitation
This version collects and exports labeled examples. It does **not yet train or run an AI model**, analyze live gameplay, or execute taps/swipes. Those need a separate vision/decision model and an explicit, user-controlled Android screen-capture/accessibility implementation. Do not treat saved examples as a trained model.

## Build
Run the **Android APK** workflow on the main branch. Download the `Game-Shaders-debug-apk` artifact from the successful workflow run.
