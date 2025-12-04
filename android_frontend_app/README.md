# StreamTranslate Android App

This Android app streams HLS (.m3u8) content using ExoPlayer, parses subtitles in real time (embedded or sidecar VTT), and displays either original or translated subtitles with an overlay UI. It includes a pluggable on-device translation stub to simulate edge AI translation.

Features
- HLS streaming via ExoPlayer
- Real-time subtitle display and processing
- Toggle between original/translated or show both lines
- Language selection (source and target)
- On-device translation stub with adjustable latency
- DataStore persistence for stream URL and settings
- Ocean Professional theme (blue and amber accents)

How to run
1) Build:
   ./gradlew build

2) Install and run on device/emulator:
   ./gradlew :app:installDebug

3) Launch "StreamTranslate".

Change stream URL
- Open Settings (top-right gear icon) and set your HLS URL (e.g., https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8).
- Optionally, provide a sidecar VTT URL.

Translator stub
- A pluggable Translator interface is provided under app/src/main/kotlin/org/example/app/translation.
- TranslationStub simulates latency and prefixes "[Translated] ".
- You can inject a custom Translator via TranslationManager.injectTranslator for testing.

Notes
- No external API keys required.
- Uses Android Views (no Compose).
- Keep translations on-device; replace TranslationStub with a real model when ready.
