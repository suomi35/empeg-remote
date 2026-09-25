# Ghostwheel Empeg Fixtures

Captured read-only (HTTP GET only, no commands sent) from the live community
player at `https://www.ghostwheel.de/empeg` on 2026-09-24. The player runs
**empeg web lite 0.95** (hijack-based web server) and serves **XML**, not the
legacy HTML tables that the old EmpegRemote app scraped.

## Contents

- `playlists/FID_*.xml` — playlist responses captured by `crawl.sh`
  (BFS from root FID=101, max depth 5, 1s delay, playlist links only).
  - `FID_101.xml` — root ("All Music"): mixed playlist + tune items
  - `FID_171.xml` — "Misc" playlist (sub-playlists + tunes)
  - `FID_1712.xml`, `FID_2e31.xml`, `FID_91e1.xml`, `FID_f281.xml` — leaf playlists
- `default.xsl` — the stylesheet that renders the XML in a browser; documents
  the full URL grammar (see below).
- `weblite/weblite.js`, `weblite/domresize.js`, `weblite/weblite.css` — the
  web lite UI source; authoritative reference for command URLs.
- `weblite_index.html` — the `weblite/` directory listing (fascia list).
- `edge_bad_FID.xml` — response for `?FID=9999&EXT=.xml`: **HTTP 200 with an
  EMPTY body** (not a 404). Parsers must handle this.
- `edge_root.m3u` — requesting `.m3u` from this host redirects back to the
  playlist XML (streaming endpoint behaves differently here than on stock
  emplayer; treat with caution).
- `screen_sample.gif` — **404 page**. `/proc/empeg_screen.gif` is referenced
  by the web UI but disabled/absent on this player. On a real player with
  hijack screen-grab enabled it returns a live 256x64 GIF of the VFD.

## Fonts (`fonts/*.bf`)

The player renders its VFD with its own **"EFNT" font files** from
`/empeg/lib/fonts/` on the player's disk. This directory holds the authentic
set, extracted **offline from an official firmware image** (no player needed):

```
tools/extract_player_fonts_from_upgrade.py \
    ~/Development/empeg/original-player-firmware-main/player-firmware/car-v2.00/car2-consumer-v2.00.upgrade \
    fixtures/ghostwheel/fonts
```

| file | glyph height | glyphs | role |
| --- | --- | --- | --- |
| `small.bf` | 6px | 231 (chars 25..255) | menu/status text |
| `medium.bf` | 9px | 231 (chars 25..255) | menu text (simulator default) |
| `large.bf` | 18px | 231 (chars 25..255) | now-playing text |
| `graphics.bf` | 9px | 14-21 (digits/punctuation) | digits and symbols only |
| `graphics_large.bf` | 16px | 30 | big digits/graphics |
| `wait.bf` | 15px | 6 (`A`..`F`) | spinner segments |
| `timecode.bf` | 21px | 14 (digits, `:`) | timecode display |

Format: 32-byte `EFNT` header (file size, version, max width, 32 bits per
scanline, glyph height, first char, char count), then per glyph a 32-bit
width and `height` scanlines of 32-bit words holding **2-bit shades,
LSB-first** (bits 0-1 = leftmost pixel; 0 blank, 3 brightest). Version 2
files add a glyph-count/offset pair and a ushort mapping table. The parser is
`simulator/src/main/kotlin/empegsim/EmpegBfFont.kt`; the offline extractor
cracks the `.upgrade` container (chunked, gzipped ext2 drive image) in
`tools/extract_player_fonts_from_upgrade.py`.

With these present the simulator renders the screen as a grayscale PNG using
the player's real variable-width shaded glyphs (now playing in `medium.bf`,
or `large.bf` + `small.bf` with `--font=large`); without them it falls back to
the 1-bit hijack kfont.

To copy fonts off a **real player** instead (hijack FTP), use
`tools/fetch_player_fonts.sh <player-host-or-ip> ghostwheel`.

## URL grammar (as observed)

- Browse playlist: `?FID=<tagfid>&EXT=.xml` (root FID=101). `EXT=.htm` is
  ignored — XML is always returned.
- Item commands (via XSL links):
  - Play: `?NODATA&SERIAL=%23<fid>` (`#fid`)
  - Append: `?NODATA&SERIAL=%23<fid>%2B` (`#fid+`)
  - Insert: `?NODATA&SERIAL=%23<fid>!` (`#fid!`)
  - Enqueue: `?NODATA&SERIAL=%23<fid>-` (`#fid-`)
- Buttons (weblite.js): press = `?NODATA&BUTTONRAW=<Button>`,
  release = `?NODATA&BUTTONRAW=<Button>.R`
  (e.g. `Top`, `Left`, `Right`, `Bottom`, `Knob`, `KnobLeft`, `KnobRight`).
  Note: different from the legacy `/proc/empeg_notify?button=` endpoint the
  old app used; an emulator should support both.
- Display: `/proc/empeg_screen.gif` (256x64 VFD image; 404 when disabled).
- Response format: `text/xml; charset=iso-8859-1` (ISO-8859-1, not UTF-8 —
  mind the encoding when parsing).
- XML fields per item: `type` (`playlist` | `tune`), `tagfid`, `fid`, `year`,
  `options`, `genre`, `title`, `artist`, `source`, `comment`, `length`
  (bytes for tunes, item count for playlists), plus for tunes: `tracknr`,
  `bitrate`, `samplerate`, `codec`, `duration` (h:mm:ss), `offset`.
- Playlist root element attributes: `allow_files`, `allow_commands`, `host`,
  `type`, `tagfid`, `fid`, `title`, etc.

## Re-running the capture

`./crawl.sh` (uses macOS-compatible bash; adjust BASE to re-target).
