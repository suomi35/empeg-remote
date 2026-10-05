# Empeg Remote

An Android app for controlling an [empeg car MP3 player](https://en.wikipedia.org/wiki/Empeg_Car)
over the network: player discovery (UDP), browsing/playing the playlist tree
over HTTP, and a remote-control screen that mirrors the player's fascia.

## Repository layout

| Path | What it is |
|---|---|
| `app/` | The Android application |
| `simulator/` | A plain-JVM empeg player simulator (HTTP + UDP discovery) for development without hardware |
| `fixtures/ghostwheel/` | Responses **captured from a real player** (see its README for provenance) |
| `tools/` | Scripts for collecting assets (fonts, playlists) from a real player |

## Branches

- **`dev`** — stable baseline. Start here.
- **`cline/2f98d`** — current development tip: player simulator, authentic VFD
  fonts, injectable API layer, XML playlist support, UI fixes, multi-homed
  network discovery fixes.
- **`archive/connect-fascia`** — frozen snapshot of an abandoned/parallel line
  of work (discovery→connect rename, fascia screen, swipe actions, extra
  dialogs) that was never merged. Kept for reference only.

## Verification status

**Automated tests: green.** `./gradlew :app:testDebugUnitTest :simulator:test`
covers the wire format, discovery parsing and the XML playlist parser against
fixtures captured from a **real empeg player** (empeg web lite 0.95 / hijack
firmware — see `fixtures/ghostwheel/README.md` for capture details).

**Not yet verified: against physical hardware.** The following needs a pass on
a real device before being considered done:

- [ ] Discovery finds the player within ~2s of tapping Search (both a real
      device and `./gradlew :simulator:run` should pass this)
- [ ] Playlists tab populates from the device's XML (`GET /?FID=101&EXT=.xml`)
      — this was the "blank Playlists tab" fix in `3acf03c`
- [ ] Browsing into a sub-playlist and playing a track end-to-end
- [ ] Edge case: unknown FID returns HTTP 200 + empty body — app should show
      the empty/unreachable state, not crash
- [ ] Manual player entry (`host:port`) connects to a real device

The XML playlist code paths were implemented against *captured responses from
real hardware* and are exercised by the simulator, but the device's own
firmware version and local network behaviour have not been tested directly.

## Development

```sh
# unit tests
./gradlew :app:testDebugUnitTest :simulator:test

# run the player simulator (serves captured fixtures; app can connect to it)
./gradlew :simulator:run
# options: --port=8080 --bind=<addr> --fixtures=<dir> --name=EmpegSim --font=medium
```

JDK 17 is required (Gradle toolchain; also pinned machine-wide in
`~/.gradle/gradle.properties`, not in this repo).
